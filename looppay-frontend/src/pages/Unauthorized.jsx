import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import Button from '../components/common/Button';
import { useAuth } from '../context/AuthContext';

const Unauthorized = () => {
  const { role } = useAuth();
  const homePath = role === 'CUSTOMER' ? '/portal' : '/admin';

  return (
    <div className="min-h-screen bg-silver flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-modal p-8 border border-silver-border text-center">
        <div className="w-16 h-16 rounded-2xl bg-status-danger-bg text-status-danger flex items-center justify-center mx-auto mb-4 border border-status-danger/20">
          <ShieldAlert className="w-8 h-8" />
        </div>

        <h2 className="text-2xl font-bold text-aubergine">Access Restricted</h2>
        <p className="text-xs text-neutral-500 mt-2 leading-relaxed">
          You do not have the required role permissions to view this resource. Your access attempt has been logged according to LoopPay security policy.
        </p>

        <div className="mt-6">
          <Link to={homePath}>
            <Button variant="primary" className="w-full" icon={ArrowLeft}>
              Return to Safe Dashboard
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Unauthorized;

