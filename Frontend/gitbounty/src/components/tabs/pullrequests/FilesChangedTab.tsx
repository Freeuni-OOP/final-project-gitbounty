import { useEffect, useState } from 'react';
import { pullRequestApi } from '../../../services/pullRequestService';
import { parseUnifiedDiff, type DiffFile } from '../../../utils/parseUnifiedDiff';
import DiffFileCard from './DiffFileCard';
import '../../../styles/FilesChangedTab.css';

interface FilesChangedTabProps {
    repositoryName: string;
    prNumber: number;
}

export default function FilesChangedTab({ repositoryName, prNumber }: Readonly<FilesChangedTabProps>) {
    const [files, setFiles] = useState<DiffFile[] | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // One worker for the tab's lifetime, shared across every file's diff —
    // same reasoning as RepositoryPage's SyntaxHighlighter: recreating it per
    // file would throw away Shiki's cached highlighter/grammars.
    const [worker, setWorker] = useState<Worker | null>(null);

    useEffect(() => {
        const w = new Worker(new URL('../../../workers/shiki_worker.ts', import.meta.url), { type: 'module' });
        setWorker(w);
        return () => w.terminate();
    }, []);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError(null);

        pullRequestApi.getPullRequestDiff(repositoryName, prNumber)
            .then((response) => {
                if (!cancelled) setFiles(parseUnifiedDiff(response.rawDiff));
            })
            .catch((err: any) => {
                if (!cancelled) {
                    const status = err?.response?.status;
                    if (status === 401 || status === 403) {
                        setError('You must be signed in to view this diff.');
                    } else if (status === 404) {
                        setError('Pull request not found.');
                    } else {
                        setError(err?.message || 'Failed to load the diff.');
                    }
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [repositoryName, prNumber]);

    if (loading) {
        return (
            <div className="files-changed-tab">
                <div className="files-changed-skeleton" aria-busy="true" aria-label="Loading files changed">
                    {[0, 1, 2].map((i) => (
                        <div className="files-changed-skeleton-row" key={i} />
                    ))}
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="files-changed-tab">
                <p className="files-changed-error">{error}</p>
            </div>
        );
    }

    if (!files || files.length === 0) {
        return (
            <div className="files-changed-tab">
                <p className="files-changed-empty">No files changed.</p>
            </div>
        );
    }

    const totalAdditions = files.reduce((sum, f) => sum + f.additions, 0);
    const totalDeletions = files.reduce((sum, f) => sum + f.deletions, 0);

    return (
        <div className="files-changed-tab">
            <div className="files-changed-summary">
                <strong>{files.length}</strong> {files.length === 1 ? 'file' : 'files'} changed
                <span className="diff-additions">+{totalAdditions}</span>
                <span className="diff-deletions">-{totalDeletions}</span>
            </div>
            <div className="files-changed-list">
                {files.map((file) => (
                    <DiffFileCard key={`${file.oldPath}→${file.newPath}`} file={file} worker={worker} />
                ))}
            </div>
        </div>
    );
}
