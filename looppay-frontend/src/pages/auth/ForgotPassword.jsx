import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, AlertCircle, CheckCircle, ArrowLeft } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import api from '../../api/axios';
import { validateEmail } from '../../utils/validators';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Email address is required.');
      return;
    }
    if (!validateEmail(email)) {
      setError('Please provide a valid email address.');
      return;
    }

    setLoading(true);

    try {
      await api.post('/api/auth/forgot-password', {
        email: email.trim(),
      });
      setSubmitted(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Reset password"
      subtitle="Enter your email to receive password recovery instructions"
    >
      {error && (
        <div className="mb-4 p-3.5 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-2.5 text-xs text-status-danger font-medium">
          <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {submitted ? (
        <div className="space-y-4">
          <div className="p-4 bg-status-success-bg border border-status-success/30 rounded-xl flex items-start gap-3">
            <CheckCircle className="w-5 h-5 text-status-success flex-shrink-0 mt-0.5" />
            <div className="text-xs text-status-success">
              <p className="font-semibold text-sm">Check your email</p>
              <p className="mt-1">
                If an account with <strong>{email}</strong> exists, we've sent it a link to create a new
                password. The link works once and expires in 1 hour.
              </p>
            </div>
          </div>

          <div className="pt-2 flex flex-col gap-2">
            <Button variant="outline" className="w-full" onClick={() => setSubmitted(false)}>
              Didn't get it? Send again
            </Button>

            <Link to="/login" className="flex items-center justify-center gap-1.5 text-xs font-semibold text-aubergine hover:text-marigold py-2">
              <ArrowLeft className="w-3.5 h-3.5" /> Back to Sign In
            </Link>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Email Address"
            id="email"
            name="email"
            type="email"
            placeholder="name@company.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={error}
            icon={Mail}
            required
            helperText="We'll email you a link to reset your password."
          />

          <Button
            type="submit"
            variant="primary"
            size="lg"
            loading={loading}
            className="w-full mt-2"
          >
            Send Recovery Link
          </Button>

          <div className="pt-2 text-center">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-aubergine hover:text-marigold transition"
            >
              <ArrowLeft className="w-3.5 h-3.5" /> Back to Sign In
            </Link>
          </div>
        </form>
      )}
    </AuthLayout>
  );
};

export default ForgotPassword;

