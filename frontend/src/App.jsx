import React from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';

function AppContent() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar />
      <main className="flex-1">
        {user ? <DashboardPage /> : <LoginPage />}
      </main>
      <footer className="border-t border-slate-900 py-6 text-center text-xs text-slate-600">
        AIVES System · Nâng cấp kiến trúc Spring Boot 3 & React JS (SWD392)
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
