import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Mail, Lock, AlertCircle, Info } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import api from '../../api/axios';
import { useAuth } from '../../context/AuthContext';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    const newErrors = {};

    if (!email.trim()) newErrors.email = 'Email address is required.';
    if (!password) newErrors.password = 'Password is required.';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    setErrors({});
    setLoading(true);

    try {
      const response = await api.post('/api/auth/login', {
        email: email.trim(),
        password,
      });

      // Expected response envelope: { success: true, message: "...", data: { token, ... } }
      const authData = response.data.data;
      login(authData);

      // Redirect based on role
      const role = authData.role;
      const redirectPath = location.state?.from?.pathname || (role === 'CUSTOMER' ? '/portal' : '/admin');
      navigate(redirectPath, { replace: true });

    } catch (err) {
      if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('Unable to connect to the server. Please ensure the backend is running.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Quick fill helper for testing
  const fillSample = (sampleEmail, samplePass) => {
    setEmail(sampleEmail);
    setPassword(samplePass);
    setErrors({});
    setGeneralError('');
  };

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to your LoopPay account to continue"
    >
      {generalError && (
        <div className="mb-4 p-3.5 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-2.5 text-xs text-status-danger font-medium animate-in fade-in">
          <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{generalError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Email Address"
          id="email"
          name="email"
          type="email"
          placeholder="name@company.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
          icon={Mail}
          required
        />

        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label htmlFor="password" className="block text-sm font-semibold text-aubergine">
              Password <span className="text-status-danger">*</span>
            </label>
            <Link
              to="/forgot-password"
              className="text-xs font-semibold text-aubergine hover:text-marigold transition"
            >
              Forgot password?
            </Link>
          </div>
          <Input
            id="password"
            name="password"
            type="password"
            placeholder="••••••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            icon={Lock}
            required
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          loading={loading}
          className="w-full mt-2"
        >
          Sign In
        </Button>
      </form>

      {/* Demo Credentials Box */}
      <div className="mt-6 p-3 bg-silver-light rounded-xl border border-silver-border/70 text-xs text-neutral-600">
        <div className="flex items-center gap-1.5 font-semibold text-aubergine mb-1.5">
          <Info className="w-3.5 h-3.5 text-marigold" />
          <span>Quick Demo Login:</span>
        </div>
        <div className="flex items-center justify-between gap-2">
          <span>Admin: <strong className="text-aubergine">admin@looppay.com</strong></span>
          <button
            type="button"
            onClick={() => fillSample('admin@looppay.com', 'Admin@1234')}
            className="text-[11px] font-semibold text-aubergine bg-silver px-2 py-0.5 rounded hover:bg-marigold transition"
          >
            Auto-fill
          </button>
        </div>
      </div>

      <p className="mt-6 text-center text-xs text-neutral-500">
        Don't have an account?{' '}
        <Link to="/signup" className="font-semibold text-aubergine hover:text-marigold transition">
          Sign up as Customer
        </Link>
      </p>
    </AuthLayout>
  );
};

export default Login;

