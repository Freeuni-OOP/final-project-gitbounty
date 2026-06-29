import React, {useEffect, useRef, useState} from 'react';
import { useParams, Link } from 'react-router-dom';
import { getFileTree, mockRepositories } from '../mocks/repositoriesMock';
import type { FileNode } from '../mocks/repositoriesMock';
import IssuesTab from '../components/IssuesTab';
import PullRequestsTab from '../components/PullRequestsTab';
import BountiesTab from '../components/BountiesTab';
import '../styles/RepositoryPage.css';
import Prism from 'prismjs';
import 'prismjs/components/prism-java'; // Tells Prism how to read Java
import 'prismjs/themes/prism-tomorrow.css'; // A nice dark mode style

type Tab = 'Code' | 'Issues' | 'Pull Requests' | 'Bounties';
const TABS: Tab[] = ['Code', 'Issues', 'Pull Requests', 'Bounties'];

function sortEntries(entries: FileNode[]): FileNode[] {
  return [...entries].sort((a, b) => {
    if (a.type !== b.type) return a.type === 'dir' ? -1 : 1;
    return a.name.localeCompare(b.name);
  });
}

function getDir(tree: FileNode[], path: string[]): FileNode[] {
  let cur = tree;
  for (const seg of path) {
    const node = cur.find((n) => n.name === seg && n.type === 'dir');
    cur = node?.children ?? [];
  }
  return sortEntries(cur);
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
  }, [content, isDarkMode]); // Re-runs coloring when text or theme changes

  // Swapping the theme stylesheets dynamically
  const themeUrl = isDarkMode
      ? "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css" // Dark
      : "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css";          // Light

  return (
      <>
        <link rel="stylesheet" href={themeUrl} />
        <pre className="file-content">
        <code ref={codeRef} className="language-java">
          {content}
        </code>
      </pre>
      </>
  );
}

export default function RepositoryPage() {
  const { owner = '', repoName = '' } = useParams<{ owner: string; repoName: string }>();
  const [activeTab, setActiveTab] = useState<Tab>('Code');
  const [path, setPath] = useState<string[]>([]);
  const [selectedFile, setSelectedFile] = useState<FileNode | null>(null);

  const [isDarkBox, setIsDarkBox] = useState<boolean>(true);
  const repo = mockRepositories.find((r) => r.owner === owner && r.name === repoName);
  const tree = getFileTree(owner, repoName);
  const entries = getDir(tree, path);

  const openDir = (node: FileNode) => { setPath((p) => [...p, node.name]); setSelectedFile(null); };
  const openFile = (node: FileNode) => setSelectedFile(node);
  const navTo = (idx: number) => { setPath((p) => p.slice(0, idx)); setSelectedFile(null); };
  const goRoot = () => { setPath([]); setSelectedFile(null); };

  const switchTab = (tab: Tab) => { setActiveTab(tab); goRoot(); };

  if (!repo) {
    return (
      <div className="repo-not-found">
        <p>Repository <strong>{owner}/{repoName}</strong> not found.</p>
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
      {activeTab === 'Issues' && <IssuesTab owner={owner} repoName={repoName} />}
      {activeTab === 'Pull Requests' && <PullRequestsTab repoName={repoName} />}
      {activeTab === 'Bounties' && <BountiesTab owner={owner} repoName={repoName} />}
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

          {selectedFile ? (
              <div className="file-viewer">
                <div className="file-viewer-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span className="file-viewer-name">{selectedFile.name}</span>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span className="file-viewer-lines">
          {(selectedFile.content ?? '').split('\n').filter(Boolean).length} lines
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
                          color: '#24292e'
                        }}
                    >
                      {isDarkBox ? '☀️ Light Mode' : '🌙 Dark Mode'}
                    </button>
                  </div>
                </div>

                <SyntaxHighlighter content={selectedFile.content ?? ''} isDarkMode={isDarkBox} />
              </div>
          ) : (
            <table className="file-table">
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.name} className="file-row">
                    <td className="file-icon-cell">
                      {entry.type === 'dir' ? <FolderIcon /> : <FileIcon />}
                    </td>
                    <td className="file-name-cell">
                      <button
                        className="file-name-btn"
                        onClick={() => entry.type === 'dir' ? openDir(entry) : openFile(entry)}
                      >
                        {entry.name}
                      </button>
                    </td>
                    <td className="file-commit-cell">{entry.lastCommit}</td>
                    <td className="file-time-cell">{entry.commitTime}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
