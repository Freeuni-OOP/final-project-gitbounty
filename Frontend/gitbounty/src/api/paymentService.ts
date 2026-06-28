import apiClient from './apiClient';

export const paymentService = {
    // Send the credit card details and credits to purchase to the backend
    createTopUp: async (paymentData: any) => {
        const response = await apiClient.post('/api/payments/credit-topups', paymentData);
        return response.data;
    },

    // Fetch the current logged-in user's token/credit balance
    getMyBalance: async () => {
        const response = await apiClient.get('/api/payments/me/balance');
        return response.data;
    },

    // Fetch a history of all payments made by this user
    getMyPayments: async () => {
        const response = await apiClient.get('/api/payments/me');
        return response.data;
    }
};