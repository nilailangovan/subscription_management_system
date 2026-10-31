import React, { useState } from 'react';
import { UserPlus, Users, Shield, Plus } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import PasswordRequirementList from '../../components/common/PasswordRequirementList';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { validatePassword, validateEmail } from '../../utils/validators';

const StaffManagement = () => {
  const { user, role } = useAuth();
  const isAdmin = role === 'ADMIN';

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // List of staff
  const [staffMembers, setStaffMembers] = useState([
    {
      id: 1,
      firstName: 'Default',
      lastName: 'Admin',
      email: 'admin@looppay.com',
      role: 'ADMIN',
      active: true,
      createdAt: '2026-10-31',
    },
  ]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setGeneralError('');
    setSuccess('');

    const newErrors = {};
    if (!formData.firstName.trim()) newErrors.firstName = 'First name is required.';
    if (!formData.lastName.trim()) newErrors.lastName = 'Last name is required.';
    if (!formData.email.trim()) {
      newErrors.email = 'Email address is required.';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Enter a valid email address.';
    }

    const pwdCheck = validatePassword(formData.password);
    if (!pwdCheck.isValid) {
      newErrors.password = pwdCheck.errorMessage;
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);

    try {
      const response = await api.post('/api/admin/users/staff', {
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        email: formData.email.trim(),
        password: formData.password,
      });

      const newMember = response.data.data;
      setStaffMembers((prev) => [newMember, ...prev]);
      setSuccess(`Staff user ${newMember.email} created successfully.`);
      setIsModalOpen(false);
      setFormData({ firstName: '', lastName: '', email: '', password: '' });
    } catch (err) {
      if (err.response?.data?.errors) {
        setErrors(err.response.data.errors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('Failed to create staff account.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <div>
          <h2 className="text-2xl font-bold text-aubergine tracking-tight">Staff & Team Management</h2>
          <p className="text-xs text-neutral-500 mt-1">
            Authorize operational team members to manage customer billing and subscriptions.
          </p>
        </div>

        {isAdmin && (
          <Button variant="primary" onClick={() => setIsModalOpen(true)} icon={Plus}>
            Add Staff Member
          </Button>
        )}
      </div>

      {success && (
        <div className="p-4 bg-status-success-bg border border-status-success/30 rounded-xl text-xs text-status-success font-semibold">
          {success}
        </div>
      )}

      <Card title="Authorized Personnel" subtitle="Users with administrative or operational clearance">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-aubergine">
            <thead className="bg-silver-light text-neutral-600 uppercase font-semibold border-b border-silver-border">
              <tr>
                <th className="px-5 py-3.5">Name</th>
                <th className="px-5 py-3.5">Email</th>
                <th className="px-5 py-3.5">Role</th>
                <th className="px-5 py-3.5">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-silver-border/60">
              {staffMembers.map((member, idx) => (
                <tr key={idx} className="hover:bg-silver-light/40">
                  <td className="px-5 py-4 font-semibold">
                    {member.firstName} {member.lastName}
                  </td>
                  <td className="px-5 py-4 text-neutral-600">{member.email}</td>
                  <td className="px-5 py-4">
                    <Badge status={member.role} size="sm" />
                  </td>
                  <td className="px-5 py-4">
                    <Badge status={member.active ? 'ACTIVE' : 'CLOSED'} size="sm" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add Staff Member"
        subtitle="Staff members can review billing periods and subscriptions"
      >
        {generalError && (
          <div className="mb-4 p-3 bg-status-danger-bg border border-status-danger/30 rounded-xl text-xs text-status-danger font-medium">
            {generalError}
          </div>
        )}

        <form onSubmit={handleCreate} className="space-y-3.5">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="First Name"
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              error={errors.firstName}
              required
            />
            <Input
              label="Last Name"
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              error={errors.lastName}
              required
            />
          </div>

          <Input
            label="Email Address"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            error={errors.email}
            required
          />

          <div>
            <Input
              label="Password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              error={errors.password}
              required
            />
            <PasswordRequirementList password={formData.password} />
          </div>

          <div className="pt-3 flex items-center justify-end gap-2.5">
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" loading={loading} icon={UserPlus}>
              Create Staff
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default StaffManagement;

