import React, { useState } from 'react';
import {
  DollarSign,
  Repeat,
  FileText,
  Percent,
  UserPlus,
  Shield,
  Users,
  CheckCircle,
  AlertCircle
} from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import PasswordRequirementList from '../../components/common/PasswordRequirementList';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { validatePassword, validateEmail } from '../../utils/validators';

const AdminDashboard = () => {
  const { user, role } = useAuth();
  const isAdmin = role === 'ADMIN';

  // Add Staff Modal & Form State
  const [isStaffModalOpen, setIsStaffModalOpen] = useState(false);
  const [staffForm, setStaffForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });
  const [formErrors, setFormErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [successNotice, setSuccessNotice] = useState('');
  const [loading, setLoading] = useState(false);

  // Recently created staff state (client tracking)
  const [createdStaffList, setCreatedStaffList] = useState([]);

  const handleStaffChange = (e) => {
    const { name, value } = e.target;
    setStaffForm((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleCreateStaff = async (e) => {
    e.preventDefault();
    setGeneralError('');
    setSuccessNotice('');

    const errors = {};
    if (!staffForm.firstName.trim()) errors.firstName = 'First name is required.';
    if (!staffForm.lastName.trim()) errors.lastName = 'Last name is required.';
    if (!staffForm.email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!validateEmail(staffForm.email)) {
      errors.email = 'Enter a valid email address.';
    }

    const pwdCheck = validatePassword(staffForm.password);
    if (!pwdCheck.isValid) {
      errors.password = pwdCheck.errorMessage;
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setLoading(true);

    try {
      const response = await api.post('/api/admin/users/staff', {
        firstName: staffForm.firstName.trim(),
        lastName: staffForm.lastName.trim(),
        email: staffForm.email.trim(),
        password: staffForm.password,
      });

      const newStaff = response.data.data;
      setCreatedStaffList((prev) => [newStaff, ...prev]);
      setSuccessNotice(`Staff user '${newStaff.email}' was created successfully!`);

      // Reset form and close modal
      setStaffForm({ firstName: '', lastName: '', email: '', password: '' });
      setFormErrors({});
      setIsStaffModalOpen(false);

    } catch (err) {
      if (err.response?.data?.errors) {
        setFormErrors(err.response.data.errors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('Failed to create staff account. Please verify admin permissions.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border/80">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-2xl font-bold text-aubergine tracking-tight">
              Console Overview
            </h2>
            <Badge status={role} />
          </div>
          <p className="text-xs text-neutral-500 mt-1">
            Logged in as <strong className="text-aubergine">{user?.email}</strong> &bull; LoopPay Billing & Subscription Engine
          </p>
        </div>

        {isAdmin && (
          <Button
            variant="primary"
            onClick={() => {
              setGeneralError('');
              setIsStaffModalOpen(true);
            }}
            icon={UserPlus}
          >
            Add New Staff
          </Button>
        )}
      </div>

      {/* Success Notification */}
      {successNotice && (
        <div className="p-4 bg-status-success-bg border border-status-success/30 rounded-xl flex items-center justify-between text-xs text-status-success font-medium animate-in fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 flex-shrink-0" />
            <span>{successNotice}</span>
          </div>
          <button
            onClick={() => setSuccessNotice('')}
            className="text-xs uppercase font-bold text-status-success hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* StatCards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Monthly Recurring Revenue"
          value="$24,850"
          change="+12.4%"
          isPositive={true}
          icon={DollarSign}
          helperText="Vs. previous billing cycle"
        />
        <StatCard
          title="Active Subscriptions"
          value="148"
          change="+8.2%"
          isPositive={true}
          icon={Repeat}
          helperText="Zero churn in last 30 days"
        />
        <StatCard
          title="Pending Invoices"
          value="14"
          change="-3.1%"
          isPositive={true}
          icon={FileText}
          helperText="Awaiting automatic settlement"
        />
        <StatCard
          title="Collection Rate"
          value="98.2%"
          change="+0.5%"
          isPositive={true}
          icon={Percent}
          helperText="Idempotent auto-retry active"
        />
      </div>

      {/* Main Grid: Add Staff Quick Access & Team Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Staff Management Card */}
        <div className="lg:col-span-2 space-y-6">
          <Card
            title="Team & Operational Staff"
            subtitle="Manage operational staff accounts authorized to oversee subscriptions"
            action={
              isAdmin ? (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsStaffModalOpen(true)}
                  icon={UserPlus}
                >
                  Create Staff
                </Button>
              ) : null
            }
          >
            {createdStaffList.length === 0 ? (
              <div className="text-center py-10 px-4">
                <div className="w-12 h-12 rounded-full bg-silver-light flex items-center justify-center mx-auto text-neutral-400 mb-3">
                  <Users className="w-6 h-6" />
                </div>
                <h4 className="text-sm font-semibold text-aubergine">No newly created staff members</h4>
                <p className="text-xs text-neutral-500 mt-1 max-w-sm mx-auto">
                  Click the <strong>Add New Staff</strong> button above to invite staff members to LoopPay.
                </p>
                {isAdmin && (
                  <Button
                    variant="primary"
                    size="sm"
                    className="mt-4"
                    onClick={() => setIsStaffModalOpen(true)}
                    icon={UserPlus}
                  >
                    Add Staff Member
                  </Button>
                )}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-aubergine">
                  <thead className="bg-silver-light text-neutral-600 uppercase font-semibold border-b border-silver-border">
                    <tr>
                      <th className="px-4 py-2.5">Name</th>
                      <th className="px-4 py-2.5">Email</th>
                      <th className="px-4 py-2.5">Role</th>
                      <th className="px-4 py-2.5">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-silver-border/60">
                    {createdStaffList.map((staff, idx) => (
                      <tr key={idx} className="hover:bg-silver-light/40">
                        <td className="px-4 py-3 font-semibold">
                          {staff.firstName} {staff.lastName}
                        </td>
                        <td className="px-4 py-3 text-neutral-600">{staff.email}</td>
                        <td className="px-4 py-3">
                          <Badge status="STAFF" size="sm" />
                        </td>
                        <td className="px-4 py-3">
                          <Badge status={staff.active ? 'ACTIVE' : 'CLOSED'} size="sm" />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {/* Quick Notice: Phase 2 info */}
          <div className="p-4 bg-white rounded-xl border border-silver-border flex items-start gap-3.5 shadow-2xs">
            <div className="w-8 h-8 rounded-lg bg-marigold/20 text-aubergine flex items-center justify-center flex-shrink-0 mt-0.5">
              <Repeat className="w-4 h-4" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-aubergine uppercase tracking-wide">
                Phase 2 Modules Ready to Plug In
              </h4>
              <p className="text-xs text-neutral-500 mt-0.5 leading-relaxed">
                Products, Subscription Plans, Quotations, and Idempotent Invoicing engines will be attached in the next phases per project roadmap.
              </p>
            </div>
          </div>
        </div>

        {/* Right Column: System Security & Role Summary */}
        <div className="space-y-6">
          <Card title="Security & RBAC" subtitle="Role-based enforcement status">
            <div className="space-y-3.5 text-xs">
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border/80 flex items-start gap-3">
                <Shield className="w-4 h-4 text-status-success flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-aubergine">@PreAuthorize Enforced</p>
                  <p className="text-[11px] text-neutral-500 mt-0.5">
                    Spring Security validates JWT claims for ADMIN & STAFF roles on every request.
                  </p>
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-silver/60">
                <p className="font-semibold text-[11px] uppercase tracking-wider text-neutral-400">
                  Active Access Roles:
                </p>
                <div className="flex items-center justify-between p-2 rounded-lg bg-white border border-silver-border">
                  <span className="font-medium text-aubergine">ADMIN</span>
                  <Badge status="ADMIN" size="sm" />
                </div>
                <div className="flex items-center justify-between p-2 rounded-lg bg-white border border-silver-border">
                  <span className="font-medium text-aubergine">STAFF</span>
                  <Badge status="STAFF" size="sm" />
                </div>
                <div className="flex items-center justify-between p-2 rounded-lg bg-white border border-silver-border">
                  <span className="font-medium text-aubergine">CUSTOMER</span>
                  <Badge status="CUSTOMER" size="sm" />
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Add Staff Modal Dialog */}
      <Modal
        isOpen={isStaffModalOpen}
        onClose={() => setIsStaffModalOpen(false)}
        title="Add Operational Staff Member"
        subtitle="Staff members can manage customer subscriptions and review invoices"
      >
        {generalError && (
          <div className="mb-4 p-3 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-2 text-xs text-status-danger font-medium">
            <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            <span>{generalError}</span>
          </div>
        )}

        <form onSubmit={handleCreateStaff} className="space-y-3.5">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="First Name"
              name="firstName"
              placeholder="Mark"
              value={staffForm.firstName}
              onChange={handleStaffChange}
              error={formErrors.firstName}
              required
            />
            <Input
              label="Last Name"
              name="lastName"
              placeholder="Staff"
              value={staffForm.lastName}
              onChange={handleStaffChange}
              error={formErrors.lastName}
              required
            />
          </div>

          <Input
            label="Email Address"
            name="email"
            type="email"
            placeholder="mark.staff@looppay.com"
            value={staffForm.email}
            onChange={handleStaffChange}
            error={formErrors.email}
            required
            helperText="Must be unique across all LoopPay accounts."
          />

          <div>
            <Input
              label="Temporary Password"
              name="password"
              type="password"
              placeholder="Set strong initial password"
              value={staffForm.password}
              onChange={handleStaffChange}
              error={formErrors.password}
              required
            />
            <PasswordRequirementList password={staffForm.password} />
          </div>

          <div className="pt-3 flex items-center justify-end gap-2.5">
            <Button
              variant="ghost"
              type="button"
              onClick={() => setIsStaffModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              type="submit"
              loading={loading}
              icon={UserPlus}
            >
              Create Staff User
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default AdminDashboard;

