import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, AlertCircle, CheckCircle, ArrowLeft, ExternalLink, HelpCircle, UserX } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import api from '../../api/axios';
import { validateEmail } from '../../utils/validators';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [token, setToken] = useState(null);
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
      const res = await api.post('/api/auth/forgot-password', {
        email: email.trim(),
      });
      // The API returns the generated token in data during local development
      const generatedToken = res.data?.data;
      setToken(generatedToken || null);
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
          {token ? (
            <>
              {/* Token Generated Success */}
              <div className="p-4 bg-status-success-bg border border-status-success/30 rounded-xl flex items-start gap-3">
                <CheckCircle className="w-5 h-5 text-status-success flex-shrink-0 mt-0.5" />
                <div className="text-xs text-status-success">
                  <p className="font-semibold text-sm">Reset Link Generated!</p>
                  <p className="mt-1">
                    Password recovery token generated for <strong>{email}</strong>.
                  </p>
                </div>
              </div>

              {/* Dev Direct Action Box */}
              <div className="p-4 bg-silver-light border border-silver-border rounded-xl space-y-2.5 text-xs">
                <div className="flex items-center gap-2 text-aubergine font-bold">
                  <span>🚀 1-Click Password Reset (Local Mode)</span>
                </div>
                <p className="text-[11px] text-neutral-600">
                  Because this is running on localhost without an external SMTP mail server configured, your reset link is ready right here:
                </p>

                <div className="pt-1">
                  <Link to={`/reset-password?token=${token}`}>
                    <Button variant="primary" className="w-full flex items-center justify-center gap-2 py-2.5">
                      <span>Reset Password for {email}</span>
                      <ExternalLink className="w-4 h-4" />
                    </Button>
                  </Link>
                </div>
              </div>
            </>
          ) : (
            <>
              {/* Email Not Found Notice */}
              <div className="p-4 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-3">
                <UserX className="w-5 h-5 text-status-danger flex-shrink-0 mt-0.5" />
                <div className="text-xs text-status-danger">
                  <p className="font-semibold text-sm">No Account Found</p>
                  <p className="mt-1">
                    No registered user exists with email <strong>{email}</strong>.
                  </p>
                </div>
              </div>

              <div className="p-4 bg-silver-light border border-silver-border rounded-xl space-y-2 text-xs text-neutral-600">
                <p className="font-semibold text-aubergine">Registered Test Accounts:</p>
                <ul className="list-disc pl-4 space-y-1 text-[11px]">
                  <li><code>admin@looppay.com</code> (System Administrator)</li>
                  <li><code>jane.doe@example.com</code> (Customer)</li>
                </ul>
                <p className="text-[11px] mt-2">
                  Want to use your own email? Sign up below first!
                </p>
              </div>

              <div className="pt-2 flex flex-col gap-2">
                <Link to="/signup">
                  <Button variant="primary" className="w-full">
                    Create New Account with {email}
                  </Button>
                </Link>
                <Button variant="outline" onClick={() => setSubmitted(false)} className="w-full">
                  Try Another Email
                </Button>
              </div>
            </>
          )}

          <div className="pt-2 text-center">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-aubergine hover:text-marigold transition"
            >
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
            placeholder="e.g. admin@looppay.com or your email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={error}
            icon={Mail}
            required
            helperText="Enter your registered account email."
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
