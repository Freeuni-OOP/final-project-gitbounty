import { Link } from 'react-router-dom';

const NotFoundPage = () => {
  return (
    <div style={{ textAlign: 'center', padding: '4rem 2rem' }}>
      <h1 style={{ fontSize: '6rem', margin: 0, lineHeight: 1 }}>404</h1>
      <h2 style={{ fontSize: '1.5rem', marginTop: '1rem' }}>Page Not Found</h2>
      <p style={{ color: '#666', marginTop: '0.5rem' }}>
        The page you're looking for doesn't exist.
      </p>
      <Link
        to="/"
        style={{
          display: 'inline-block',
          marginTop: '1.5rem',
          padding: '0.6rem 1.4rem',
          background: '#1a73e8',
          color: '#fff',
          borderRadius: '6px',
          textDecoration: 'none',
        }}
      >
        Back to Home
      </Link>
    </div>
  );
};

export default NotFoundPage;
