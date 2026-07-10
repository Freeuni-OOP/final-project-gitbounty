import { useMemo, useState } from 'react';
import type { DiffFile, DiffLine } from '../../../utils/parseUnifiedDiff';
import { useDiffHighlight } from '../../../hooks/useDiffHighlight';

// Files with more changed lines than this are collapsed by default, mirroring
// GitHub's behavior of hiding large diffs behind a click instead of choking
// the page on render. Everything else is expanded, matching how the repo
// Code tab always shows full file content without a collapse step.
const LARGE_FILE_LINE_THRESHOLD = 800;

function countChangedLines(file: DiffFile): number {
    return file.hunks.reduce((sum, hunk) => sum + hunk.lines.length, 0);
}

function statusLabel(status: DiffFile['status']): string {
    switch (status) {
        case 'added': return 'Added';
        case 'deleted': return 'Deleted';
        case 'renamed': return 'Renamed';
        default: return 'Modified';
    }
}

interface RenderRow {
    key: string;
    kind: 'hunk-header' | 'line' | 'no-newline';
    headerText?: string;
    lineType?: 'add' | 'del' | 'context';
    oldLineNumber?: number | null;
    newLineNumber?: number | null;
    content?: string;
    html?: string;
}

function buildRows(file: DiffFile, oldLines: string[] | null, newLines: string[] | null): RenderRow[] {
    const rows: RenderRow[] = [];
    let oldIdx = 0;
    let newIdx = 0;

    file.hunks.forEach((hunk, hunkIndex) => {
        rows.push({ key: `h${hunkIndex}`, kind: 'hunk-header', headerText: hunk.header });

        hunk.lines.forEach((line: DiffLine, lineIndex) => {
            const key = `h${hunkIndex}-l${lineIndex}`;
            if (line.type === 'no-newline') {
                rows.push({ key, kind: 'no-newline', content: line.content });
                return;
            }

            let html: string | undefined;
            if (line.type === 'del') {
                html = oldLines?.[oldIdx];
                oldIdx += 1;
            } else if (line.type === 'add') {
                html = newLines?.[newIdx];
                newIdx += 1;
            } else {
                html = newLines?.[newIdx];
                oldIdx += 1;
                newIdx += 1;
            }

            rows.push({
                key,
                kind: 'line',
                lineType: line.type,
                oldLineNumber: line.oldLineNumber,
                newLineNumber: line.newLineNumber,
                content: line.content,
                html,
            });
        });
    });

    return rows;
}

interface DiffFileCardProps {
    file: DiffFile;
    worker: Worker | null;
}

export default function DiffFileCard({ file, worker }: Readonly<DiffFileCardProps>) {
    const isLarge = useMemo(() => countChangedLines(file) > LARGE_FILE_LINE_THRESHOLD, [file]);
    const [collapsed, setCollapsed] = useState(isLarge);

    const { oldLines, newLines } = useDiffHighlight(file, worker, false, !collapsed);
    const rows = useMemo(
        () => (collapsed ? [] : buildRows(file, oldLines, newLines)),
        [collapsed, file, oldLines, newLines],
    );

    const displayPath = file.status === 'renamed' && file.oldPath !== file.newPath
        ? `${file.oldPath} → ${file.newPath}`
        : (file.newPath || file.oldPath);

    return (
        <div className="diff-file-card">
            <div className="diff-file-header">
                <button
                    type="button"
                    className="diff-file-collapse-btn"
                    onClick={() => setCollapsed((c) => !c)}
                    aria-expanded={!collapsed}
                    aria-label={collapsed ? `Expand ${displayPath}` : `Collapse ${displayPath}`}
                >
                    <svg
                        width="12" height="12" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"
                        className={`diff-chevron ${collapsed ? 'collapsed' : ''}`}
                    >
                        <path d="M12.78 6.22a.75.75 0 0 1 0 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L3.22 7.28a.75.75 0 0 1 1.06-1.06L8 9.94l3.72-3.72a.75.75 0 0 1 1.06 0Z" />
                    </svg>
                </button>
                <span className={`diff-status-badge ${file.status}`}>{statusLabel(file.status)}</span>
                <span className="diff-file-path" title={displayPath}>{displayPath}</span>
                <span className="diff-file-stats">
                    <span className="diff-additions">+{file.additions}</span>
                    <span className="diff-deletions">-{file.deletions}</span>
                </span>
            </div>

            {!collapsed && (
                <div className="diff-file-body">
                    {file.isBinary ? (
                        <div className="diff-file-note">Binary file not shown.</div>
                    ) : rows.length === 0 ? (
                        <div className="diff-file-note">No content changes.</div>
                    ) : (
                        <table className="diff-table">
                            <tbody>
                                {rows.map((row) => {
                                    if (row.kind === 'hunk-header') {
                                        return (
                                            <tr key={row.key} className="diff-hunk-row">
                                                <td colSpan={3} className="diff-hunk-header">{row.headerText}</td>
                                            </tr>
                                        );
                                    }
                                    if (row.kind === 'no-newline') {
                                        return (
                                            <tr key={row.key} className="diff-line-row diff-line-meta">
                                                <td className="diff-gutter" />
                                                <td className="diff-gutter" />
                                                <td className="diff-content-cell">
                                                    <span className="diff-meta-text">{row.content}</span>
                                                </td>
                                            </tr>
                                        );
                                    }
                                    const marker = row.lineType === 'add' ? '+' : row.lineType === 'del' ? '-' : '';
                                    return (
                                        <tr key={row.key} className={`diff-line-row diff-line-${row.lineType}`}>
                                            <td className="diff-gutter diff-gutter-old">{row.oldLineNumber ?? ''}</td>
                                            <td className="diff-gutter diff-gutter-new">{row.newLineNumber ?? ''}</td>
                                            <td className="diff-content-cell">
                                                <span className="diff-marker">{marker}</span>
                                                {row.html !== undefined ? (
                                                    <span className="diff-code" dangerouslySetInnerHTML={{ __html: row.html }} />
                                                ) : (
                                                    <span className="diff-code diff-code-plain">{row.content}</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    )}
                </div>
            )}
        </div>
    );
}
