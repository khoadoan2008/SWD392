import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';

// Admin Pages
import SubjectsPage from './pages/admin/SubjectsPage';
import UsersPage from './pages/admin/UsersPage';
import ConfigsPage from './pages/admin/ConfigsPage';

// Lecturer Pages
import QuestionsPage from './pages/lecturer/QuestionsPage';
import QuestionFormPage from './pages/lecturer/QuestionFormPage';
import QuestionGeneratePage from './pages/lecturer/QuestionGeneratePage';
import RubricsPage from './pages/lecturer/RubricsPage';
import SessionsPage from './pages/lecturer/SessionsPage';
import SessionDetailPage from './pages/lecturer/SessionDetailPage';

// Student Pages
import InterviewHomePage from './pages/student/InterviewHomePage';
import InterviewRoomPage from './pages/student/InterviewRoomPage';

// Protected Route Wrapper
function ProtectedRoute({ children, allowedRoles }) {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

function AppContent() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      <Navbar />
      <main className="flex-1">
        <Routes>
          {/* Public / Auth */}
          <Route
            path="/login"
            element={user ? <Navigate to="/" replace /> : <LoginPage />}
          />

          {/* Dashboard */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />

          {/* Admin Routes */}
          <Route
            path="/admin/subjects"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <SubjectsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <UsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/configs"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <ConfigsPage />
              </ProtectedRoute>
            }
          />

          {/* Lecturer Routes */}
          <Route
            path="/lecturer/questions"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <QuestionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lecturer/questions/new"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <QuestionFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lecturer/questions/:id/edit"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <QuestionFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lecturer/questions/generate"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <QuestionGeneratePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lecturer/rubrics"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <RubricsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lecturer/sessions"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <SessionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lecturer/sessions/:id"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'LECTURER']}>
                <SessionDetailPage />
              </ProtectedRoute>
            }
          />

          {/* Student Routes */}
          <Route
            path="/student/interview"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'STUDENT']}>
                <InterviewHomePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/interview/:id"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'STUDENT']}>
                <InterviewRoomPage />
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="border-t border-slate-900 py-6 text-center text-xs text-slate-500">
        AIVES System · Hệ thống Thi Vấn đáp Thông minh (Spring Boot 3 + ReactJS SPA) · SWD392
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </BrowserRouter>
  );
}
