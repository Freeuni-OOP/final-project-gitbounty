import {useAuth} from "../../providers/AuthProvider.tsx";

export const SignInButton = () => {
    const { authenticated, login, logout, isLoading } = useAuth();

    // The UI now reacts to the loading state
    if (isLoading) {
        return <button className="nav-button-secondary" disabled>Loading...</button>;
    }

    const handleAuth = () => {
        authenticated ? logout() : login();
    };

    return (
        <button className="nav-button-secondary" onClick={handleAuth}>
            {authenticated ? 'Sign Out' : 'Sign In'}
        </button>
    );
};