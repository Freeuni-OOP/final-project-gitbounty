import { useEffect, useRef, useState } from 'react';
import './BranchDropdown.css';

interface BranchDropdownProps {
  branches: string[];
  currentBranch?: string;
  onBranchChange?: (branch: string) => void;
  loading?: boolean;
  error?: string | null;
}

export default function BranchDropdown({
  branches,
  currentBranch = 'main',
  onBranchChange,
  loading = false,
  error = null,
}: Readonly<BranchDropdownProps>) {
  const [open, setOpen] = useState(false);
  const [selectedBranch, setSelectedBranch] = useState(currentBranch);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    if (!open) return;
    const onMouseDown = (e: MouseEvent) => {
      if (!wrapperRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [open]);

  const handleBranchSelect = (branch: string) => {
    setSelectedBranch(branch);
    onBranchChange?.(branch);
    setOpen(false);
  };

  return (
    <div className="branch-dropdown-wrapper" ref={wrapperRef}>
      <button
        className="branch-dropdown-btn"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        aria-label="Select branch"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 16 16"
          fill="currentColor"
          aria-hidden="true"
        >
          <path d="M9.5 1.75a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm0 5a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm0 5a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm1-11.5a2.25 2.25 0 1 0-4.5 0A2.25 2.25 0 0 0 10.5 2.25ZM8 11.5a.5.5 0 1 0-1 0 .5.5 0 0 0 1 0Zm1-11.5a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm-.75 2.5a.75.75 0 0 1 .75.75v5a.75.75 0 0 1-1.5 0v-5a.75.75 0 0 1 .75-.75Z" />
        </svg>
        {selectedBranch}
        <svg
          width="12"
          height="12"
          viewBox="0 0 16 16"
          fill="currentColor"
          aria-hidden="true"
        >
          <path d="M4.427 7.427l3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 7H4.604a.25.25 0 0 0-.177.427Z" />
        </svg>
      </button>

      {open && (
        <div className="branch-dropdown-menu">
          <div className="branch-dropdown-header">
            <p className="branch-dropdown-label">Switch branches</p>
          </div>

          {error && (
            <div className="branch-dropdown-error">
              <p>{error}</p>
            </div>
          )}

          {loading && (
            <div className="branch-dropdown-loading">
              <p>Loading branches…</p>
            </div>
          )}

          {!loading && !error && branches.length > 0 && (
            <div className="branch-dropdown-list">
              {branches.map((branch) => (
                <button
                  key={branch}
                  className={`branch-dropdown-item ${
                    branch === selectedBranch ? 'active' : ''
                  }`}
                  onClick={() => handleBranchSelect(branch)}
                >
                  <svg
                    width="12"
                    height="12"
                    viewBox="0 0 16 16"
                    fill="currentColor"
                    aria-hidden="true"
                    className="branch-icon"
                  >
                    <path d="M9.5 1.75C9.5 .784 10.284 0 11.25 0c.966 0 1.75.784 1.75 1.75 0 .699-.409 1.304-1 1.605v2.348c0 .349-.114.677-.31.951.825.186 1.822.65 2.757 1.584.636.636.999 1.505 1.004 2.41v2.2c.591.301 1 .906 1 1.605 0 .966-.784 1.75-1.75 1.75-.966 0-1.75-.784-1.75-1.75 0-.699.409-1.304 1-1.605v-2.2c-.005-.315-.108-.612-.315-.82-.936-.936-2.057-1.351-2.986-1.351-.929 0-2.05.415-2.986 1.35-.206.209-.31.506-.315.821v2.2c.591.301 1 .906 1 1.605 0 .966-.784 1.75-1.75 1.75-.966 0-1.75-.784-1.75-1.75 0-.699.409-1.304 1-1.605V8.628c0-.349-.114-.677-.31-.951A2.987 2.987 0 0 0 5.75 5.703V3.355c-.591-.301-1-.906-1-1.605C4.75 .784 5.534 0 6.5 0 7.466 0 8.25.784 8.25 1.75c0 .699-.409 1.304-1 1.605v2.348c0 .349.114.677.31.951.825.186 1.822.65 2.757 1.584.636.636.999 1.505 1.004 2.41v2.2c.591.301 1 .906 1 1.605 0 .966-.784 1.75-1.75 1.75-.966 0-1.75-.784-1.75-1.75 0-.699.409-1.304 1-1.605v-2.2c-.005-.315-.108-.612-.315-.82-.936-.936-2.057-1.351-2.986-1.351-.929 0-2.05.415-2.986 1.35-.206.209-.31.506-.315.821v2.2c.591.301 1 .906 1 1.605 0 .966-.784 1.75-1.75 1.75-.966 0-1.75-.784-1.75-1.75 0-.699.409-1.304 1-1.605V8.628c0-.349-.114-.677-.31-.951A2.987 2.987 0 0 0 1.75 5.703V3.355C1.159 3.054.75 2.449.75 1.75.75.784 1.534 0 2.5 0 3.466 0 4.25.784 4.25 1.75Z" />
                  </svg>
                  <span className="branch-name">{branch}</span>
                  {branch === selectedBranch && (
                    <svg
                      width="12"
                      height="12"
                      viewBox="0 0 16 16"
                      fill="currentColor"
                      aria-hidden="true"
                      className="checkmark"
                    >
                      <path d="M13.78 4.22a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 11-1.06-1.06l7.25-7.25a.75.75 0 011.06 0Z" />
                    </svg>
                  )}
                </button>
              ))}
            </div>
          )}

          {!loading && !error && branches.length === 0 && (
            <div className="branch-dropdown-empty">
              <p>No branches found</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
