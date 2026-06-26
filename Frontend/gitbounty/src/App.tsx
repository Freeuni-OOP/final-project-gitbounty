import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';
import BountiesPage from './pages/BountiesPage';
import RepositoriesPage from './pages/RepositoriesPage';
import RepositoryPage from './pages/RepositoryPage';

function App() {
  return (
    <Router>
      <Navbar />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/bounties" element={<BountiesPage />} />
        <Route path="/repositories" element={<RepositoriesPage />} />
        <Route path="/repositories/:owner/:repoName" element={<RepositoryPage />} />
      </Routes>
    </Router>
  );
}

export default App;
