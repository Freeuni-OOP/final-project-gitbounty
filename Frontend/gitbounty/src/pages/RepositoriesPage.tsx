import { useRepositoriesData } from '../hooks/useRepositoriesData';
import { RepoCard } from '../components/RepoCard';
import '../styles/RepositoriesPage.css';
import { useEffect, useRef } from 'react';
import { useNavigationType } from 'react-router-dom';

const SCROLL_KEY = 'repositoriesScrollY';

export default function RepositoriesPage() {
    const { repos, isLoading, error } = useRepositoriesData();
    const navigationType = useNavigationType(); // 'POP' | 'PUSH' | 'REPLACE'
    const hasRestoredRef = useRef(false);

    // Continuously persist scroll position, throttled to once per animation frame.
    // (Deliberately not relying on unmount cleanup to capture this, since StrictMode's
    // simulated unmount/remount on every mount will mess with a cleanup-based save.)
    useEffect(() => {
        let ticking = false;
        const handleScroll = () => {
            if (ticking) return;
            ticking = true;
            requestAnimationFrame(() => {
                sessionStorage.setItem(SCROLL_KEY, window.scrollY.toString());
                ticking = false;
            });
        };

        window.addEventListener('scroll', handleScroll, { passive: true });
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    // Restore scroll position, but only when arriving here via browser back/forward
    useEffect(() => {
        if (isLoading || hasRestoredRef.current) return;
        if (navigationType !== 'POP') return;

        const savedScrollY = sessionStorage.getItem(SCROLL_KEY);
        if (!savedScrollY) return;

        hasRestoredRef.current = true;

        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                window.scrollTo(0, parseInt(savedScrollY, 10));
            });
        });
    }, [isLoading, navigationType]);

  if (isLoading) {
    return (
      <div className="repositories-page">
        <p className="repositories-loading">Loading repositories…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="repositories-page">
        <p className="repositories-error">Failed to load repositories: {error}</p>
      </div>
    );
  }

  return (
    <div className="repositories-page">
      <div className="repositories-header">
        <h1 className="repositories-title">Repositories</h1>
        <p className="repositories-subtitle">{repos.length} repositories</p>
      </div>
      <div className="repositories-list">
        {repos.map((repo) => (
          <RepoCard key={repo.id} repo={repo} />
        ))}
      </div>
    </div>
  );
}
