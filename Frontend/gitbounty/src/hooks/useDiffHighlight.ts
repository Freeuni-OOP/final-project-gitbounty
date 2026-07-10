import { useEffect, useMemo, useState } from 'react';
import type { DiffFile, DiffLine } from '../utils/parseUnifiedDiff';

// Same Shiki-worker approach as RepositoryPage's SyntaxHighlighter, adapted
// for diffs: the raw diff only gives us line-level +/-/context markers, not
// full file contents, so each side of a file's diff is reassembled into its
// own contiguous "old" and "new" text blocks (in the order those lines
// appear across the file's hunks) and highlighted as one unit each. That
// keeps multi-line constructs (block comments, template strings, ...)
// tokenized correctly, instead of highlighting every diff line in isolation.

const HIGHLIGHT_CACHE_LIMIT = 30;
const highlightCache = new Map<string, string[]>();

function cacheKey(filename: string, isDarkMode: boolean, content: string): string {
    return `${isDarkMode ? 'dark' : 'light'}|${filename}|${content}`;
}

function setCached(key: string, lines: string[]) {
    if (highlightCache.size >= HIGHLIGHT_CACHE_LIMIT) {
        const oldestKey = highlightCache.keys().next().value;
        if (oldestKey !== undefined) highlightCache.delete(oldestKey);
    }
    highlightCache.set(key, lines);
}

// Shiki wraps every source line in its own <span class="line">; pull those
// back out so each diff row can be rendered with our own gutter/background.
function splitShikiLines(html: string): string[] {
    if (!html) return [];
    const doc = new DOMParser().parseFromString(html, 'text/html');
    return Array.from(doc.querySelectorAll('code > span.line'), (el) => el.innerHTML);
}

function buildSideText(file: DiffFile, side: 'old' | 'new'): string {
    const parts: string[] = [];
    for (const hunk of file.hunks) {
        for (const line of hunk.lines as DiffLine[]) {
            if (line.type === 'no-newline') continue;
            if (side === 'old' && (line.type === 'context' || line.type === 'del')) parts.push(line.content);
            if (side === 'new' && (line.type === 'context' || line.type === 'add')) parts.push(line.content);
        }
    }
    return parts.join('\n');
}

let globalRequestId = 0;

export interface DiffHighlight {
    // null = highlighting hasn't resolved yet; render the plain-text fallback.
    oldLines: string[] | null;
    newLines: string[] | null;
}

export function useDiffHighlight(
    file: DiffFile,
    worker: Worker | null,
    isDarkMode: boolean,
    enabled: boolean,
): DiffHighlight {
    const [oldLines, setOldLines] = useState<string[] | null>(null);
    const [newLines, setNewLines] = useState<string[] | null>(null);

    const oldText = useMemo(() => buildSideText(file, 'old'), [file]);
    const newText = useMemo(() => buildSideText(file, 'new'), [file]);

    useEffect(() => {
        // Collapsed files (and binary files, which have no text to tokenize)
        // skip highlighting entirely until/unless expanded.
        if (!enabled || file.isBinary) return;

        const oldKey = cacheKey(file.oldPath, isDarkMode, oldText);
        const newKey = cacheKey(file.newPath, isDarkMode, newText);
        const cachedOld = oldText === '' ? [] : highlightCache.get(oldKey);
        const cachedNew = newText === '' ? [] : highlightCache.get(newKey);

        if (cachedOld) setOldLines(cachedOld);
        if (cachedNew) setNewLines(cachedNew);
        if (cachedOld && cachedNew) return;
        if (!worker) return;

        const oldRequestId = cachedOld ? null : ++globalRequestId;
        const newRequestId = cachedNew ? null : ++globalRequestId;

        const handleMessage = (event: MessageEvent) => {
            const { success, html, error, requestId: respId } = event.data;
            if (respId === oldRequestId) {
                if (success) {
                    const lines = splitShikiLines(html);
                    setCached(oldKey, lines);
                    setOldLines(lines);
                } else {
                    console.error('Diff highlighting failed (old side):', error);
                    setOldLines([]);
                }
            } else if (respId === newRequestId) {
                if (success) {
                    const lines = splitShikiLines(html);
                    setCached(newKey, lines);
                    setNewLines(lines);
                } else {
                    console.error('Diff highlighting failed (new side):', error);
                    setNewLines([]);
                }
            }
        };

        worker.addEventListener('message', handleMessage);
        if (oldRequestId !== null) {
            worker.postMessage({ content: oldText, isDarkMode, filename: file.oldPath, requestId: oldRequestId });
        }
        if (newRequestId !== null) {
            worker.postMessage({ content: newText, isDarkMode, filename: file.newPath, requestId: newRequestId });
        }

        return () => {
            worker.removeEventListener('message', handleMessage);
        };
    }, [enabled, worker, file, oldText, newText, isDarkMode]);

    return { oldLines, newLines };
}
