// Parses the raw unified diff text returned by
// GET /api/codebases/{repo}/pull-requests/{prNumber}/diff (JGit DiffFormatter
// output) into a structure the Files changed tab can render. The backend
// only exposes the raw diff text (no pre-parsed per-file/per-hunk JSON), so
// this is the minimal amount of parsing needed to build GitHub-style file
// cards from it.

export type DiffFileStatus = 'added' | 'deleted' | 'modified' | 'renamed';

export type DiffLine =
    | { type: 'context' | 'add' | 'del'; content: string; oldLineNumber: number | null; newLineNumber: number | null }
    | { type: 'no-newline'; content: string };

export interface DiffHunk {
    header: string;
    lines: DiffLine[];
}

export interface DiffFile {
    oldPath: string;
    newPath: string;
    status: DiffFileStatus;
    isBinary: boolean;
    additions: number;
    deletions: number;
    hunks: DiffHunk[];
}

const HUNK_HEADER_RE = /^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)$/;
const BINARY_RE = /^Binary files (.+) and (.+) differ$/;

function stripPrefix(path: string, prefix: 'a/' | 'b/'): string {
    return path.startsWith(prefix) ? path.slice(prefix.length) : path;
}

function newFile(oldPath: string, newPath: string): DiffFile {
    return { oldPath, newPath, status: 'modified', isBinary: false, additions: 0, deletions: 0, hunks: [] };
}

export function parseUnifiedDiff(rawDiff: string): DiffFile[] {
    if (!rawDiff.trim()) return [];

    const files: DiffFile[] = [];
    let current: DiffFile | null = null;
    let currentHunk: DiffHunk | null = null;
    let oldLine = 0;
    let newLine = 0;

    const finishHunk = () => {
        if (current && currentHunk) current.hunks.push(currentHunk);
        currentHunk = null;
    };

    // A trailing "\n" in rawDiff (the normal case) produces a spurious empty
    // final element from split() that isn't a real diff line — drop it so it
    // isn't mistaken for a blank context line.
    const rawLines = rawDiff.split('\n');
    if (rawLines.length > 0 && rawLines[rawLines.length - 1] === '') rawLines.pop();

    for (const line of rawLines) {
        const gitHeaderMatch = /^diff --git a\/(.+) b\/(.+)$/.exec(line);
        if (gitHeaderMatch) {
            finishHunk();
            current = newFile(gitHeaderMatch[1], gitHeaderMatch[2]);
            files.push(current);
            continue;
        }
        if (!current) continue; // stray content before the first "diff --git" header

        if (line.startsWith('rename from ')) {
            current.oldPath = line.slice('rename from '.length);
            current.status = 'renamed';
            continue;
        }
        if (line.startsWith('rename to ')) {
            current.newPath = line.slice('rename to '.length);
            current.status = 'renamed';
            continue;
        }
        if (line.startsWith('copy from ')) {
            current.oldPath = line.slice('copy from '.length);
            current.status = 'renamed';
            continue;
        }
        if (line.startsWith('copy to ')) {
            current.newPath = line.slice('copy to '.length);
            current.status = 'renamed';
            continue;
        }
        if (line.startsWith('new file mode')) {
            current.status = 'added';
            continue;
        }
        if (line.startsWith('deleted file mode')) {
            current.status = 'deleted';
            continue;
        }
        const binaryMatch = BINARY_RE.exec(line);
        if (binaryMatch) {
            current.isBinary = true;
            current.oldPath = stripPrefix(binaryMatch[1], 'a/');
            current.newPath = stripPrefix(binaryMatch[2], 'b/');
            continue;
        }
        if (line.startsWith('--- ')) {
            const path = line.slice(4);
            if (path === '/dev/null') {
                if (current.status === 'modified') current.status = 'added';
            } else {
                current.oldPath = stripPrefix(path, 'a/');
            }
            continue;
        }
        if (line.startsWith('+++ ')) {
            const path = line.slice(4);
            if (path === '/dev/null') {
                if (current.status === 'modified') current.status = 'deleted';
            } else {
                current.newPath = stripPrefix(path, 'b/');
            }
            continue;
        }
        if (line.startsWith('old mode') || line.startsWith('new mode') || line.startsWith('similarity index')
            || line.startsWith('dissimilarity index') || line.startsWith('index ')) {
            continue;
        }

        const hunkMatch = HUNK_HEADER_RE.exec(line);
        if (hunkMatch) {
            finishHunk();
            oldLine = Number.parseInt(hunkMatch[1], 10);
            newLine = Number.parseInt(hunkMatch[3], 10);
            currentHunk = { header: line, lines: [] };
            continue;
        }
        if (!currentHunk) continue; // metadata line outside any hunk we don't need

        if (line.startsWith('\\')) {
            currentHunk.lines.push({ type: 'no-newline', content: line });
            continue;
        }
        if (line.startsWith('+')) {
            currentHunk.lines.push({ type: 'add', content: line.slice(1), oldLineNumber: null, newLineNumber: newLine });
            newLine += 1;
            current.additions += 1;
            continue;
        }
        if (line.startsWith('-')) {
            currentHunk.lines.push({ type: 'del', content: line.slice(1), oldLineNumber: oldLine, newLineNumber: null });
            oldLine += 1;
            current.deletions += 1;
            continue;
        }
        // context line: leading space, or (defensively) an empty line
        currentHunk.lines.push({ type: 'context', content: line.slice(1), oldLineNumber: oldLine, newLineNumber: newLine });
        oldLine += 1;
        newLine += 1;
    }
    finishHunk();

    return files;
}
