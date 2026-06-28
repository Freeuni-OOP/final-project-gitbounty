import { useState, useEffect } from 'react';
import { paymentService } from '../api/paymentService';
import { useAuth } from '../auth/useAuth';

function BuyCreditsPage() {
    const { isLoading: isAuthLoading, authenticated } = useAuth();

    const [formData, setFormData] = useState({
        creditsToPurchase: '100',
        cardholderName: '',
        cardNumber: '',
        expiryMonth: '1',
        expiryYear: '2026',
        cvv: ''
    });

    const [balance, setBalance] = useState<number>(0);
    const [loading, setLoading] = useState<boolean>(false);
    const [message, setMessage] = useState<{ type: string; text: string }>({ type: '', text: '' });

    const fetchPageBalance = () => {
        paymentService.getMyBalance()
            .then((res: any) => {
                const data = res?.data && res?.status ? res.data : res;
                setBalance(data?.creditBalance ?? 0);
            })
            .catch((err: any) => console.error("Could not fetch page balance", err));
    };

    useEffect(() => {
        // Don't fetch until Keycloak has actually finished initializing
        // and only if the user is logged in (This replaces the previous 400ms guess).
        if (isAuthLoading || !authenticated) return;

        fetchPageBalance();
    }, [isAuthLoading, authenticated]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const idempotencyKey = crypto.randomUUID();

            const payload = {
                creditsToPurchase: Number(formData.creditsToPurchase),
                cardholderName: formData.cardholderName,
                cardNumber: formData.cardNumber,
                expiryMonth: parseInt(formData.expiryMonth, 10),
                expiryYear: parseInt(formData.expiryYear, 10),
                cvv: formData.cvv,
                idempotencyKey: idempotencyKey
            };

            const response = await paymentService.createTopUp(payload);

            setMessage({ type: 'success', text: `Success! Purchased ${response.creditsGranted} credits.` });

            window.dispatchEvent(new Event('balanceUpdated'));

            fetchPageBalance();
        } catch (error: any) {
            const serverErrorMessage = error.response?.data?.message || "Payment failed. Please check details.";
            setMessage({ type: 'error', text: serverErrorMessage });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ backgroundColor: '#0B132B', color: '#FFFFFF', minHeight: '100vh', padding: '40px 20px' }}>
            <div style={{ maxWidth: '500px', margin: '0 auto', backgroundColor: '#1C2541', padding: '30px', borderRadius: '8px' }}>

                <h2 style={{ fontSize: '24px', marginBottom: '10px' }}>Top Up Credits</h2>
                <p style={{ color: '#5BC0BE', marginBottom: '20px' }}>Current Balance: <strong>{balance} Credits</strong></p>

                {message.text && (
                    <div style={{
                        padding: '10px',
                        borderRadius: '4px',
                        marginBottom: '20px',
                        backgroundColor: message.type === 'success' ? '#1B4332' : '#721C24',
                        color: message.type === 'success' ? '#D4EDDA' : '#F8D7DA'
                    }}>
                        {message.text}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    {/* Credits Field */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Credits to Purchase (Max 1000)</label>
                        <input
                            type="number" name="creditsToPurchase" max={1000} min={1} required
                            value={formData.creditsToPurchase} onChange={handleChange}
                            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: 'none', backgroundColor: '#3A506B', color: '#FFF' }}
                        />
                    </div>

                    {/* Cardholder Name */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Cardholder Name</label>
                        <input
                            type="text" name="cardholderName" required placeholder="John Doe"
                            value={formData.cardholderName} onChange={handleChange}
                            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: 'none', backgroundColor: '#3A506B', color: '#FFF' }}
                        />
                    </div>

                    {/* Card Number */}
                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Card Number (Mock validation active)</label>
                        <input
                            type="text" name="cardNumber" required placeholder="1234432112344321"
                            value={formData.cardNumber} onChange={handleChange}
                            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: 'none', backgroundColor: '#3A506B', color: '#FFF' }}
                        />
                    </div>

                    {/* Expiry & CVV Row */}
                    <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
                        <div style={{ flex: 1 }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>Expiry Month</label>
                            <input
                                type="number" name="expiryMonth" min={1} max={12} required placeholder="MM"
                                value={formData.expiryMonth} onChange={handleChange}
                                style={{ width: '100%', padding: '10px', borderRadius: '4px', border: 'none', backgroundColor: '#3A506B', color: '#FFF' }}
                            />
                        </div>
                        <div style={{ flex: 1 }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>Expiry Year</label>
                            <input
                                type="number" name="expiryYear" min={2026} required placeholder="YYYY"
                                value={formData.expiryYear} onChange={handleChange}
                                style={{ width: '100%', padding: '10px', borderRadius: '4px', border: 'none', backgroundColor: '#3A506B', color: '#FFF' }}
                            />
                        </div>
                        <div style={{ flex: 1 }}>
                            <label style={{ display: 'block', marginBottom: '5px' }}>CVV</label>
                            <input
                                type="text" name="cvv" required placeholder="123" maxLength={4}
                                value={formData.cvv} onChange={handleChange}
                                style={{ width: '100%', padding: '10px', borderRadius: '4px', border: 'none', backgroundColor: '#3A506B', color: '#FFF' }}
                            />
                        </div>
                    </div>

                    <button
                        type="submit" disabled={loading}
                        style={{
                            width: '100%', padding: '12px', borderRadius: '4px', border: 'none',
                            backgroundColor: '#6C5CE7', color: '#FFF', fontWeight: 'bold', cursor: 'pointer',
                            opacity: loading ? 0.6 : 1
                        }}
                    >
                        {loading ? 'Processing Mock Payment...' : 'Purchase Credits'}
                    </button>
                </form>

            </div>
        </div>
    );
}

export default BuyCreditsPage;