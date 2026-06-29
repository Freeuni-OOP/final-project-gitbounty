import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import { paymentService } from '../api/paymentService';

interface BalanceContextValue {
    balance: number;
    refreshBalance: () => void;
    setBalance: (value: number) => void;
}

const BalanceContext = createContext<BalanceContextValue | null>(null);

export function BalanceProvider({ children }: { children: ReactNode }) {
    const [balance, setBalance] = useState<number>(0);

    const refreshBalance = useCallback(() => {
        paymentService.getMyBalance()
            .then((res: any) => {
                const data = res?.data && res?.status ? res.data : res;
                setBalance(data?.creditBalance ?? 0);
            })
            .catch((err: any) => console.error("Could not fetch balance", err));
    }, []);

    return (
        <BalanceContext.Provider value={{ balance, refreshBalance, setBalance }}>
            {children}
        </BalanceContext.Provider>
    );
}

export function useBalance(): BalanceContextValue {
    const ctx = useContext(BalanceContext);
    if (!ctx) throw new Error("useBalance must be used within a BalanceProvider");
    return ctx;
}