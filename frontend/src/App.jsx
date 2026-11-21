import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import BoardPage from './pages/BoardPage';

const ProtectedRoute = ({ children }) => {
  const { user, isReady } = useAuth();

  if (!isReady) {
    return (
      <div className="min-h-screen bg-background text-muted-foreground flex items-center justify-center">
        <div className="flex items-center gap-2.5 text-sm">
          <span
            className="h-2 w-2 rounded-full bg-accent animate-pulse"
            aria-hidden="true"
          />
          <span>Loading workspace...</span>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-md focus:bg-primary focus:px-3 focus:py-2 focus:text-xs focus:font-semibold focus:text-primary-foreground"
        >
          Skip to main content
        </a>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/boards/:boardId"
            element={
              <ProtectedRoute>
                <BoardPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
