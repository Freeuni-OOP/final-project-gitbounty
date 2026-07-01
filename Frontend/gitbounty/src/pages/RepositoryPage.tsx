import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import IssuesTab from '../components/IssuesTab';
import PullRequestsTab from '../components/PullRequestsTab';
import BountiesTab from '../components/BountiesTab';
import '../styles/RepositoryPage.css';
import Prism from 'prismjs';
import 'prismjs/components/prism-java';
import 'prismjs/themes/prism-tomorrow.css';
import apiClient from "../api/apiClient.ts";
import { useProfileData } from '../hooks/useProfileData';
import { useAuth } from '../auth/useAuth';

type Tab = 'Code' | 'Issues' | 'Pull Requests' | 'Bounties';
const TABS: Tab[] = ['Code', 'Issues', 'Pull Requests', 'Bounties'];

interface RepoData {
  id: number;
  name: string;
  description: string;
  gitUrl: string;
  ownerUsername: string;
  createdAt: string;
}

interface ContentsResponse {
  type: 'FILE' | 'DIRECTORY';
  content: string | null;
  items: string[] | null;
}

function looksLikeFile(name: string): boolean {
  if (name.startsWith('.')) return false;
  const last = name.split('/').pop() ?? name;
  return last.includes('.');
}

function FolderIcon() {
  return (
    <svg className="entry-icon dir-icon" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M1.75 1A1.75 1.75 0 0 0 0 2.75v10.5C0 14.216.784 15 1.75 15h12.5A1.75 1.75 0 0 0 16 13.25v-8.5A1.75 1.75 0 0 0 14.25 3H7.5a.25.25 0 0 1-.2-.1l-.9-1.2C6.07 1.26 5.55 1 5 1H1.75Z" />
    </svg>
  );
}

function FileIcon() {
  return (
    <svg className="entry-icon file-icon" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M2 1.75C2 .784 2.784 0 3.75 0h6.586c.464 0 .909.184 1.237.513l2.914 2.914c.329.328.513.773.513 1.237v9.586A1.75 1.75 0 0 1 13.25 16h-9.5A1.75 1.75 0 0 1 2 14.25Zm1.75-.25a.25.25 0 0 0-.25.25v12.5c0 .138.112.25.25.25h9.5a.25.25 0 0 0 .25-.25V6h-2.75A1.75 1.75 0 0 1 9 4.25V1.5Zm6.75.062V4.25c0 .138.112.25.25.25h2.688l-.011-.013-2.914-2.914-.013-.011Z" />
    </svg>
  );
}

function CloneButton({ gitUrl }: Readonly<{ gitUrl: string }>) {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onMouseDown = (e: MouseEvent) => {
      if (!wrapperRef.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [open]);

  const copy = () => {
    navigator.clipboard.writeText(gitUrl).catch(() => {});
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="clone-wrapper" ref={wrapperRef}>
      <button className="clone-btn" onClick={() => setOpen(o => !o)}>
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="m11.28 3.22 4.25 4.25a.749.749 0 0 1 0 1.06l-4.25 4.25a.749.749 0 1 1-1.06-1.06L13.94 8l-3.72-3.72a.749.749 0 1 1 1.06-1.06Zm-6.56 0a.749.749 0 1 1 1.06 1.06L2.06 8l3.72 3.72a.749.749 0 1 1-1.06 1.06L.47 8.53a.749.749 0 0 1 0-1.06Z" />
        </svg>
        Code
        <svg width="10" height="10" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M4.427 7.427l3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 7H4.604a.25.25 0 0 0-.177.427Z" />
        </svg>
      </button>
      {open && (
        <div className="clone-dropdown">
          <p className="clone-label">Clone</p>
          <div className="clone-url-row">
            <input
              className="clone-url-input"
              value={gitUrl}
              readOnly
              onClick={e => (e.target as HTMLInputElement).select()}
            />
            <button
              className={`clone-copy-btn${copied ? ' copied' : ''}`}
              onClick={copy}
              aria-label="Copy clone URL"
            >
              {copied ? 'Copied!' : (
                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
                  <path d="M0 6.75C0 5.784.784 5 1.75 5h1.5a.75.75 0 0 1 0 1.5h-1.5a.25.25 0 0 0-.25.25v7.5c0 .138.112.25.25.25h7.5a.25.25 0 0 0 .25-.25v-1.5a.75.75 0 0 1 1.5 0v1.5A1.75 1.75 0 0 1 9.25 16h-7.5A1.75 1.75 0 0 1 0 14.25Z" />
                  <path d="M5 1.75C5 .784 5.784 0 6.75 0h7.5C15.216 0 16 .784 16 1.75v7.5A1.75 1.75 0 0 1 14.25 11h-7.5A1.75 1.75 0 0 1 5 9.25Zm1.75-.25a.25.25 0 0 0-.25.25v7.5c0 .138.112.25.25.25h7.5a.25.25 0 0 0 .25-.25v-7.5a.25.25 0 0 0-.25-.25Z" />
                </svg>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

interface HighlighterProps {
  content: string;
  isDarkMode: boolean;
}

function SyntaxHighlighter({ content, isDarkMode }: Readonly<HighlighterProps>) {
  const codeRef = useRef<HTMLElement>(null);

  useEffect(() => {
    if (codeRef.current) {
      Prism.highlightElement(codeRef.current);
    }
  }, [content, isDarkMode]);

  const themeUrl = isDarkMode
    ? 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css'
    : 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css';

  const lines = content.split('\n');
  // drop trailing blank line that split creates when content ends with \n
  const lineCount = lines.length > 0 && lines.at(-1) === ''
    ? lines.length - 1
    : lines.length;

  return (
    <>
      <link rel="stylesheet" href={themeUrl} />
      <div className="code-viewer" data-theme={isDarkMode ? 'dark' : 'light'}>
        <pre className="line-numbers" aria-hidden="true">
          {Array.from({ length: lineCount }, (_, i) => i + 1).join('\n')}
        </pre>
        <pre className="file-content">
          <code ref={codeRef} className="language-java">{content}</code>
        </pre>
      </div>
    </>
  );
}

export default function RepositoryPage() {
  const { owner = '', repoName = '' } =
      useParams<{
        owner: string;
        repoName: string;
      }>();

  const { user: currentUser } = useProfileData();
  const { authenticated } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>('Code');
  const [isDarkBox, setIsDarkBox] = useState(true);

  // Repo metadata
  const [repo, setRepo] = useState<RepoData | null>(null);
  const [repoLoading, setRepoLoading] = useState(true);
  const [repoError, setRepoError] = useState<string | null>(null);

  // Code browser state
  const [path, setPath] = useState<string[]>([]);
  const [dirItems, setDirItems] = useState<string[]>([]);
  const [selectedFile, setSelectedFile] = useState<{ name: string; content: string } | null>(null);
  const [contentsLoading, setContentsLoading] = useState(false);
  const [contentsError, setContentsError] = useState<string | null>(null);

  // Fetch repo metadata
  useEffect(() => {
    if (!repoName) return;
    let cancelled = false;
    setRepoLoading(true);

    apiClient.get<RepoData>(`api/codebases/${repoName}`)
        .then(response => {
          if (!cancelled) {
            setRepo(response.data);
            setRepoLoading(false);
          }
        })
        .catch((err: any) => {
          if (!cancelled) {
            // Axios errors store response statuses/messages under err.response
            const statusStr = err.response?.status?.toString() || err.message;
            setRepoError(statusStr);
            setRepoLoading(false);
          }
        });

    return () => {
      cancelled = true;
    };
  }, [repoName]);

  // Navigate to a directory path and fetch its contents
  // Navigate to a directory path and fetch its contents
  const fetchDir = useCallback(async (newPath: string[]) => {
    const pathStr = newPath.join('/');
    const url = pathStr
        ? `/api/codebases/${repoName}/contents/${pathStr}`
        : `/api/codebases/${repoName}/contents`;
    setContentsLoading(true);
    setContentsError(null);
    setSelectedFile(null);
    try {
      const response = await apiClient.get<ContentsResponse>(url);
      setDirItems(response.data.items ?? []);
      setPath(newPath);
    } catch (err: any) {
      const statusStr = err.response?.status?.toString() || err.message;
      setContentsError(statusStr);
    } finally {
      setContentsLoading(false);
    }
  }, [repoName]);

  // Initial directory load
  useEffect(() => { fetchDir([]); }, [fetchDir]);

  const handleItemClick = async (name: string) => {
    const newPath = [...path, name];
    const url = `/api/codebases/${repoName}/contents/${newPath.join('/')}`;
    setContentsLoading(true);
    setContentsError(null);
    try {
      const response = await apiClient.get<ContentsResponse>(url);
      const data = response.data;

      if (data.type === 'FILE') {
        setSelectedFile({ name, content: data.content ?? '' });
      } else {
        setDirItems(data.items ?? []);
        setPath(newPath);
        setSelectedFile(null);
      }
    } catch (err: any) {
      const statusStr = err.response?.status?.toString() || err.message;
      setContentsError(statusStr);
    } finally {
      setContentsLoading(false);
    }
  };
  const navTo = (idx: number) => fetchDir(path.slice(0, idx));
  const goRoot = () => fetchDir([]);
  const switchTab = (tab: Tab) => { setActiveTab(tab); goRoot(); };

  if (repoLoading) {
    return <div className="repo-not-found"><p>Loading repository…</p></div>;
  }

  if (repoError || !repo) {
    return (
      <div className="repo-not-found">
        <p>Repository <strong>{repoName}</strong> not found.</p>
        <Link to="/repositories">Back to repositories</Link>
      </div>
    );
  }

  return (
    <div className="repo-page">
      {/* ── Header ── */}
      <div className="repo-header">
        <div className="repo-title-row">
          <Link to="/repositories" className="repo-owner-link">{owner}</Link>
          <span className="repo-sep">/</span>
          <span className="repo-name">{repoName}</span>
          <span className="repo-visibility-badge">Public</span>
          <CloneButton gitUrl={repo.gitUrl} />
        </div>
        {repo.description && <p className="repo-description">{repo.description}</p>}

        {/* ── Tab bar ── */}
        <div className="repo-tabs">
          {TABS.map((tab) => (
            <button
              key={tab}
              className={`repo-tab ${activeTab === tab ? 'active' : ''}`}
              onClick={() => switchTab(tab)}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {/* ── Tab content ── */}
      {activeTab === 'Issues' && (
          <IssuesTab
              repoId={repo.id}
              repoName={repoName}
              canCreateIssues={authenticated}
              canCreateBounties={
                  currentUser?.username === repo.ownerUsername
              }
          />
      )}
      {activeTab === 'Pull Requests' && <PullRequestsTab repoName={repoName} />}
      {activeTab === 'Bounties' && <BountiesTab repoId={repo.id.toString()} />}
      {activeTab === 'Code' && (
        <div className="repo-browser">
          <div className="breadcrumb">
            <button className="breadcrumb-seg link" onClick={goRoot}>{repoName}</button>
            {path.map((seg, i) => (
              <React.Fragment key={seg + i}>
                <span className="breadcrumb-slash">/</span>
                <button className="breadcrumb-seg link" onClick={() => navTo(i + 1)}>{seg}</button>
              </React.Fragment>
            ))}
            {selectedFile && (
              <>
                <span className="breadcrumb-slash">/</span>
                <span className="breadcrumb-seg">{selectedFile.name}</span>
              </>
            )}
          </div>

          {contentsError && <p className="repo-contents-loading">This repository is empty or has no content to display.</p>}
          {contentsLoading && <p className="repo-contents-loading">Loading…</p>}

          {!contentsLoading && !contentsError && selectedFile ? (
            <div className="file-viewer">
              <div className="file-viewer-header">
                <span className="file-viewer-name">{selectedFile.name}</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span className="file-viewer-lines">
                    {selectedFile.content.split('\n').filter(Boolean).length} lines
                  </span>
                  <button
                    onClick={() => setIsDarkBox(!isDarkBox)}
                    style={{
                      padding: '2px 8px',
                      fontSize: '12px',
                      cursor: 'pointer',
                      borderRadius: '4px',
                      border: '1px solid #d0d7de',
                      backgroundColor: '#f6f8fa',
                      color: '#24292e',
                    }}
                  >
                    {isDarkBox ? '☀️ Light Mode' : '🌙 Dark Mode'}
                  </button>
                </div>
              </div>
              <SyntaxHighlighter content={selectedFile.content} isDarkMode={isDarkBox} />
            </div>
          ) : !contentsLoading && !contentsError && (
            <div className="file-list">
              {dirItems.map((name) => (
                <div key={name} className="file-row">
                  <span className="file-icon-cell">
                    {looksLikeFile(name) ? <FileIcon /> : <FolderIcon />}
                  </span>
                  <button
                    className="file-name-btn"
                    onClick={() => handleItemClick(name)}
                  >
                    {name}
                  </button>
                </div>
              ))}
              {dirItems.length === 0 && (
                <div className="file-empty">This directory is empty.</div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
