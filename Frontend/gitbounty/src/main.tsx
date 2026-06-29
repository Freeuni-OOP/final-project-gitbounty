import "./auth/bootstrapAuth";
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './styles/global.css'
import {AuthContextProvider} from "./components/providers/AuthProvider.tsx";
import {BalanceProvider} from "./context/BalanceContext.tsx";

createRoot(document.getElementById('root')!).render(
  <StrictMode>
      <AuthContextProvider>
        <BalanceProvider>
          <App />
        </BalanceProvider>
      </AuthContextProvider>
  </StrictMode>,
)
