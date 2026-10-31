import React from 'react';
import { Repeat, Menu, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Badge from '../common/Badge';
import Button from '../common/Button';

/**
 * Top Navbar component:
 * - Admin/Staff mode: Minimal top bar with mobile sidebar trigger.
 * - Customer Portal mode: Full top-nav bar with LoopPay logo & customer controls.
 */
const Navbar = ({ onToggleMobileSidebar, title = 'LoopPay' }) => {
  const { user, role, logout } = useAuth();
  const isCustomer = role === 'CUSTOMER';

  if (!isCustomer) {
    // Admin / Staff top bar
    return (
      <header className="sticky top-0 z-30 h-16 bg-white/95 backdrop-blur-xs border-b border-silver-border/80 px-4 sm:px-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onToggleMobileSidebar}
            className="p-2 rounded-xl text-aubergine hover:bg-silver/50 lg:hidden focus:outline-none"
          >
            <Menu className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-bold text-aubergine tracking-tight">{title}</h1>
        </div>

        <div className="flex items-center gap-3">
          <div className="hidden sm:flex items-center gap-2">
            <span className="text-xs font-semibold text-aubergine">{user?.fullName}</span>
            <Badge status={role} size="sm" />
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={logout}
            className="text-neutral-500 hover:text-aubergine text-xs py-1.5 px-2.5"
            icon={LogOut}
          >
            <span className="hidden sm:inline">Logout</span>
          </Button>
        </div>
      </header>
    );
  }

  // Customer Portal top navigation
  return (
    <header className="sticky top-0 z-40 bg-aubergine text-white shadow-soft">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="h-16 flex items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-aubergine-light flex items-center justify-center text-marigold shadow-inner">
              <Repeat className="w-5 h-5 text-marigold" />
            </div>
            <span className="text-xl font-bold tracking-tight text-white flex items-center gap-1 font-sans">
              Loop<span className="text-marigold">Pay</span>
            </span>
            <span className="ml-2 hidden sm:inline-block text-[11px] font-semibold uppercase tracking-wider bg-aubergine-light px-2.5 py-0.5 rounded-full text-silver">
              Customer Portal
            </span>
          </div>

          {/* Navigation Links for Customer */}
          <nav className="hidden md:flex items-center gap-1">
            <a
              href="/portal"
              className="px-3.5 py-2 rounded-xl text-sm font-semibold bg-marigold text-aubergine shadow-sm"
            >
              Dashboard
            </a>
            <a
              href="#subscriptions"
              className="px-3.5 py-2 rounded-xl text-sm font-medium text-white/80 hover:text-white hover:bg-aubergine-light transition"
            >
              My Subscriptions
            </a>
            <a
              href="#invoices"
              className="px-3.5 py-2 rounded-xl text-sm font-medium text-white/80 hover:text-white hover:bg-aubergine-light transition"
            >
              My Invoices
            </a>
          </nav>

          {/* User & Logout */}
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-marigold text-aubergine font-bold flex items-center justify-center text-xs">
                {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'C'}
              </div>
              <div className="text-left">
                <p className="text-xs font-semibold text-white leading-tight">{user?.fullName}</p>
                <p className="text-[11px] text-silver-dark leading-tight">{user?.email}</p>
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={logout}
              className="text-xs py-1.5 px-3 border-white/20 text-white hover:bg-white/10"
              icon={LogOut}
            >
              <span className="hidden sm:inline">Logout</span>
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Navbar;

