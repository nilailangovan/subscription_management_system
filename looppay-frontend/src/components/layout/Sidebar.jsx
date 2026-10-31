import React from 'react';
import { NavLink } from 'react-router-dom';
import { Repeat, LayoutDashboard, Users, FileText, CreditCard, LogOut, Package, Layers, BadgePercent, Landmark } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Badge from '../common/Badge';

/**
 * Aubergine sidebar for ADMIN and STAFF users.
 * Active nav item is highlighted in marigold.
 */
const Sidebar = ({ isMobileOpen, setIsMobileOpen }) => {
  const { user, role, logout } = useAuth();

  const navItems = [
    { name: 'Dashboard', to: '/admin', icon: LayoutDashboard },
    { name: 'Staff & Team', to: '/admin/staff', icon: Users, adminOnly: true },
    { name: 'Products', to: '/admin/products', icon: Package },
    { name: 'Plans', to: '/admin/plans', icon: Layers },
    { name: 'Subscriptions', to: '/admin/subscriptions', icon: Repeat },
    { name: 'Invoices', to: '/admin/invoices', icon: FileText },
    { name: 'Payments', to: '/admin/payments', icon: CreditCard },
    { name: 'Discounts', to: '/admin/discounts', icon: BadgePercent, adminOnly: true },
    { name: 'Taxes', to: '/admin/taxes', icon: Landmark, adminOnly: true },
  ];

  return (
    <>
      {/* Mobile backdrop */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-aubergine-900/60 lg:hidden"
          onClick={() => setIsMobileOpen(false)}
        />
      )}

      <aside
        className={`
          fixed top-0 bottom-0 left-0 z-50 w-64 bg-aubergine text-white flex flex-col justify-between
          transition-transform duration-200 ease-in-out lg:translate-x-0 border-r border-aubergine-light/30
          ${isMobileOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        {/* Brand Header (scrolls on short screens so every nav link stays reachable) */}
        <div className="min-h-0 overflow-y-auto">
          <div className="h-16 flex items-center gap-2.5 px-6 border-b border-aubergine-light/40">
            <div className="w-9 h-9 rounded-xl bg-aubergine-light flex items-center justify-center text-marigold shadow-inner">
              <Repeat className="w-5 h-5 text-marigold" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-white flex items-center gap-1.5 font-sans">
                Loop<span className="text-marigold">Pay</span>
              </span>
              <span className="text-[10px] tracking-widest text-silver-dark uppercase block -mt-1 font-semibold">
                Internal Console
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="p-4 space-y-1.5">
            <p className="px-3 text-[11px] font-semibold uppercase tracking-wider text-silver-dark/60 mb-2">
              Management
            </p>

            {navItems.map((item) => {
              if (item.adminOnly && role !== 'ADMIN') return null;

              return (
                <NavLink
                  key={item.name}
                  to={item.to}
                  end={item.to === '/admin'}
                  onClick={() => setIsMobileOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition duration-150 ${
                      isActive
                        ? 'bg-marigold text-aubergine font-bold shadow-sm'
                        : 'text-white/80 hover:bg-aubergine-light hover:text-white'
                    }`
                  }
                >
                  <div className="flex items-center gap-3">
                    <item.icon className="w-4 h-4" />
                    <span>{item.name}</span>
                  </div>
                </NavLink>
              );
            })}
          </nav>
        </div>

        {/* User Profile & Sign Out Footer */}
        <div className="p-4 border-t border-aubergine-light/40 bg-aubergine-dark/40">
          <div className="flex items-center justify-between gap-2 p-2 rounded-xl bg-aubergine-light/40">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-8 h-8 rounded-lg bg-marigold text-aubergine font-bold flex items-center justify-center text-xs flex-shrink-0">
                {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold text-white truncate">{user?.fullName || 'User'}</p>
                <div className="flex items-center gap-1 mt-0.5">
                  <Badge status={role} size="sm" />
                </div>
              </div>
            </div>

            <button
              onClick={logout}
              title="Log out"
              className="p-1.5 rounded-lg text-white/70 hover:text-marigold hover:bg-aubergine-light transition focus:outline-none"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;

