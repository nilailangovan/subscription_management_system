import React from 'react';
import { Repeat, CheckCircle2, Shield, Zap } from 'lucide-react';

/**
 * Split auth layout:
 * - Centered card on silver background.
 * - Aubergine left panel with brand tagline.
 * - White right panel with form content.
 */
const AuthLayout = ({ children, title, subtitle }) => {
  return (
    <div className="min-h-screen bg-silver flex items-center justify-center p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-4xl bg-white rounded-2xl shadow-modal overflow-hidden border border-silver-border flex flex-col md:flex-row">
        {/* Left Aubergine Panel */}
        <div className="w-full md:w-5/12 bg-aubergine text-white p-8 sm:p-10 flex flex-col justify-between relative overflow-hidden">
          {/* Subtle decorative circles */}
          <div className="absolute -top-12 -left-12 w-48 h-48 rounded-full bg-aubergine-light/30 pointer-events-none blur-2xl" />
          <div className="absolute -bottom-12 -right-12 w-48 h-48 rounded-full bg-marigold/10 pointer-events-none blur-2xl" />

          {/* Top Logo */}
          <div className="relative z-10">
            <div className="flex items-center gap-2.5">
              <div className="w-10 h-10 rounded-xl bg-aubergine-light flex items-center justify-center text-marigold shadow-inner">
                <Repeat className="w-6 h-6 text-marigold" />
              </div>
              <span className="text-2xl font-bold tracking-tight text-white font-sans">
                Loop<span className="text-marigold">Pay</span>
              </span>
            </div>

            <div className="mt-8">
              <h2 className="text-2xl font-bold leading-snug tracking-tight text-white">
                Subscriptions on a loop.
                <span className="block text-marigold">Billing on autopilot.</span>
              </h2>
              <p className="mt-3 text-xs text-silver-dark leading-relaxed">
                Streamline subscription lifecycles, guarantee idempotent invoicing, and automate recurring revenue collections.
              </p>
            </div>
          </div>

          {/* Feature Highlights */}
          <div className="relative z-10 mt-8 space-y-3 pt-6 border-t border-aubergine-light/40">
            <div className="flex items-center gap-2.5 text-xs text-white/90">
              <CheckCircle2 className="w-4 h-4 text-marigold flex-shrink-0" />
              <span>Idempotent monthly & annual billing</span>
            </div>
            <div className="flex items-center gap-2.5 text-xs text-white/90">
              <Shield className="w-4 h-4 text-marigold flex-shrink-0" />
              <span>Enterprise JWT & Role-Based Access</span>
            </div>
            <div className="flex items-center gap-2.5 text-xs text-white/90">
              <Zap className="w-4 h-4 text-marigold flex-shrink-0" />
              <span>Instant customer self-service portal</span>
            </div>
          </div>

          {/* Footer note */}
          <div className="relative z-10 mt-8 text-[11px] text-white/40">
            &copy; {new Date().getFullYear()} LoopPay Software
          </div>
        </div>

        {/* Right White Form Panel */}
        <div className="w-full md:w-7/12 p-8 sm:p-10 flex flex-col justify-center bg-white">
          <div className="mb-6">
            <h3 className="text-2xl font-bold text-aubergine tracking-tight">{title}</h3>
            {subtitle && <p className="text-sm text-neutral-500 mt-1">{subtitle}</p>}
          </div>

          {children}
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;

