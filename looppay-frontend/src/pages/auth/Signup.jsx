import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, AlertCircle, CheckCircle } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import PasswordRequirementList from '../../components/common/PasswordRequirementList';
import api from '../../api/axios';
import { validatePassword, validateEmail } from '../../utils/validators';

const Signup = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    // Clear specific field error as user types
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required.';
    }
    if (!formData.lastName.trim()) {
      newErrors.lastName = 'Last name is required.';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email address is required.';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Please provide a valid email address.';
    }

    // Password validation matching RULES.md
    const pwdValidation = validatePassword(formData.password);
    if (!pwdValidation.isValid) {
      newErrors.password = pwdValidation.errorMessage;
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match.';
    }

    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    setSuccessMessage('');

    const formValidationErrors = validateForm();
    if (Object.keys(formValidationErrors).length > 0) {
      setErrors(formValidationErrors);
      return;
    }

    setErrors({});
    setLoading(true);

    try {
      const response = await api.post('/api/auth/signup', {
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        email: formData.email.trim(),
        password: formData.password,
      });

      setSuccessMessage(response.data.message || 'Account created successfully! Redirecting to login...');
      setTimeout(() => {
        navigate('/login', { state: { registeredEmail: formData.email } });
      }, 1500);

    } catch (err) {
      if (err.response?.data?.errors) {
        // Backend Jakarta validation field errors: { email: "...", password: "..." }
        setErrors(err.response.data.errors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('Registration failed. Please check your network and try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create customer account"
      subtitle="Start managing subscriptions and automated invoicing"
    >
      {generalError && (
        <div className="mb-4 p-3.5 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-2.5 text-xs text-status-danger font-medium animate-in fade-in">
          <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{generalError}</span>
        </div>
      )}

      {successMessage && (
        <div className="mb-4 p-3.5 bg-status-success-bg border border-status-success/30 rounded-xl flex items-start gap-2.5 text-xs text-status-success font-medium animate-in fade-in">
          <CheckCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{successMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3.5">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
          <Input
            label="First Name"
            name="firstName"
            placeholder="Jane"
            value={formData.firstName}
            onChange={handleChange}
            error={errors.firstName}
            icon={User}
            required
          />
          <Input
            label="Last Name"
            name="lastName"
            placeholder="Doe"
            value={formData.lastName}
            onChange={handleChange}
            error={errors.lastName}
            icon={User}
            required
          />
        </div>

        <Input
          label="Email Address"
          name="email"
          type="email"
          placeholder="jane.doe@example.com"
          value={formData.email}
          onChange={handleChange}
          error={errors.email}
          icon={Mail}
          required
        />

        <div>
          <Input
            label="Password"
            name="password"
            type="password"
            placeholder="Create strong password"
            value={formData.password}
            onChange={handleChange}
            error={errors.password}
            icon={Lock}
            required
          />
          <PasswordRequirementList password={formData.password} />
        </div>

        <Input
          label="Confirm Password"
          name="confirmPassword"
          type="password"
          placeholder="Repeat password"
          value={formData.confirmPassword}
          onChange={handleChange}
          error={errors.confirmPassword}
          icon={Lock}
          required
        />

        <Button
          type="submit"
          variant="primary"
          size="lg"
          loading={loading}
          className="w-full mt-3"
        >
          Create Account
        </Button>
      </form>

      <p className="mt-5 text-center text-xs text-neutral-500">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-aubergine hover:text-marigold transition">
          Sign In
        </Link>
      </p>
    </AuthLayout>
  );
};

export default Signup;

