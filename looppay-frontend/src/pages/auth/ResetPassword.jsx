import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { KeyRound, Lock, AlertCircle, CheckCircle, ArrowLeft, RefreshCw } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import PasswordRequirementList from '../../components/common/PasswordRequirementList';
import api from '../../api/axios';
import { validatePassword } from '../../utils/validators';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState('');
  const [hasTokenFromUrl, setHasTokenFromUrl] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  // If token is in the query params (e.g. from the reset email link ?token=xyz), auto-fill it
  useEffect(() => {
    const tokenFromUrl = searchParams.get('token');
    if (tokenFromUrl && tokenFromUrl.trim()) {
      setToken(tokenFromUrl.trim());
      setHasTokenFromUrl(true);
    }
  }, [searchParams]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    const newErrors = {};

    if (!token.trim()) {
      newErrors.token = 'Reset token is required.';
    }

    const pwdValidation = validatePassword(newPassword);
    if (!pwdValidation.isValid) {
      newErrors.newPassword = pwdValidation.errorMessage;
    }

    if (newPassword !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match.';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    setLoading(true);

    try {
      await api.post('/api/auth/reset-password', {
        token: token.trim(),
        newPassword,
      });

      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 2500);

    } catch (err) {
      if (err.response?.data?.errors) {
        setErrors(err.response.data.errors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('Password reset failed. The reset link may be expired or already used.');
      }
    } finally {
      setLoading(false);
    }
  };

  const isTokenExpiredOrUsed = generalError && (
    generalError.toLowerCase().includes('expired') ||
    generalError.toLowerCase().includes('used') ||
    generalError.toLowerCase().includes('invalid')
  );

  return (
    <AuthLayout
      title="Create new password"
      subtitle="Choose a secure new password for your account"
    >
      {generalError && (
        <div className="mb-4 p-3.5 bg-status-danger-bg border border-status-danger/30 rounded-xl space-y-2 text-xs text-status-danger font-medium animate-in fade-in">
          <div className="flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            <span>{generalError}</span>
          </div>
          {isTokenExpiredOrUsed && (
            <div className="pt-1">
              <Link to="/forgot-password">
                <Button variant="outline" size="sm" className="w-full text-xs">
                  Request a New Reset Link
                </Button>
              </Link>
            </div>
          )}
        </div>
      )}

      {success ? (
        <div className="space-y-4">
          <div className="p-4 bg-status-success-bg border border-status-success/30 rounded-xl flex items-start gap-3">
            <CheckCircle className="w-5 h-5 text-status-success flex-shrink-0 mt-0.5" />
            <div className="text-xs text-status-success">
              <p className="font-semibold text-sm">Password Updated!</p>
              <p className="mt-1">
                Your password has been changed successfully. Redirecting you to sign in...
              </p>
            </div>
          </div>

          <Link to="/login">
            <Button variant="primary" className="w-full">
              Sign In Now
            </Button>
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-3.5">
          {hasTokenFromUrl ? (
            <div className="p-3 bg-emerald-50 border border-emerald-200/80 rounded-xl flex items-center justify-between text-xs text-emerald-800">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span className="font-semibold">Reset link verified</span>
              </div>
              <span className="text-[10px] text-emerald-600 bg-emerald-100/70 px-2 py-0.5 rounded font-mono">
                token active
              </span>
            </div>
          ) : (
            <Input
              label="Reset Token"
              id="token"
              name="token"
              placeholder="Paste UUID token from email link"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              error={errors.token}
              icon={KeyRound}
              required
              helperText="Tokens expire 1 hour after request."
            />
          )}

          <div>
            <Input
              label="New Password"
              id="newPassword"
              name="newPassword"
              type="password"
              placeholder="Enter strong new password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              error={errors.newPassword}
              icon={Lock}
              required
            />
            <PasswordRequirementList password={newPassword} />
          </div>

          <Input
            label="Confirm New Password"
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            placeholder="Repeat new password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            error={errors.confirmPassword}
            icon={Lock}
            required
          />

          <Button
            type="submit"
            variant="primary"
            size="lg"
            loading={loading}
            className="w-full mt-2"
          >
            Update Password
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

export default ResetPassword;
