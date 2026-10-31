import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

/**
 * Customer portal layout with top navbar and clean centered canvas.
 */
const CustomerLayout = () => {
  return (
    <div className="min-h-screen bg-silver flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        <Outlet />
      </main>

      <footer className="py-6 text-center text-xs text-neutral-500 border-t border-silver-border">
        &copy; {new Date().getFullYear()} LoopPay Software. All rights reserved. Subscriptions on a loop. Billing on autopilot.
      </footer>
    </div>
  );
};

export default CustomerLayout;

