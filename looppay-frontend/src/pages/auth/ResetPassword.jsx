import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, AlertCircle, CheckCircle, ArrowLeft } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import PasswordRequirementList from '../../components/common/PasswordRequirementList';
import api from '../../api/axios';
import { validatePassword } from '../../utils/validators';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  // The token arrives in the emailed link: /reset-password?token=<uuid>
  const token = (searchParams.get('token') || '').trim();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    const newErrors = {};

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
        token,
        newPassword,
      });

      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 2000);

    } catch (err) {
      if (err.response?.data?.errors) {
        setErrors(err.response.data.errors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('Password reset failed. The link may be expired or already used.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create new password"
      subtitle="Choose a new password for your LoopPay account"
    >
      {generalError && (
        <div className="mb-4 p-3.5 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-2.5 text-xs text-status-danger font-medium animate-in fade-in">
          <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>
            {generalError}{' '}
            <Link to="/forgot-password" className="underline font-semibold">
              Request a new link
            </Link>
          </span>
        </div>
      )}

      {!token ? (
        <div className="space-y-4">
          <div className="p-4 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-status-danger flex-shrink-0 mt-0.5" />
            <div className="text-xs text-status-danger">
              <p className="font-semibold text-sm">This reset link is incomplete</p>
              <p className="mt-1">Open the link from your email again, or request a new one.</p>
            </div>
          </div>

          <Link to="/forgot-password">
            <Button variant="primary" className="w-full">
              Request a New Link
            </Button>
          </Link>
        </div>
      ) : success ? (
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

