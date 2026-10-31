import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Route guard component.
 * Verifies that the user is logged in, and optionally that their role is allowed.
 *
 * @param {Array<string>} allowedRoles e.g. ['ADMIN', 'STAFF'] or ['CUSTOMER']
 */
const ProtectedRoute = ({ children, allowedRoles }) => {
  const { isAuthenticated, role, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-screen bg-silver flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-aubergine border-t-marigold rounded-full animate-spin"></div>
          <p className="text-aubergine font-medium text-sm">Loading LoopPay...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to login page and remember where they were trying to go
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    // User does not have the required role
    if (role === 'CUSTOMER') {
      return <Navigate to="/portal" replace />;
    } else if (role === 'ADMIN' || role === 'STAFF') {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
};

export default ProtectedRoute;

