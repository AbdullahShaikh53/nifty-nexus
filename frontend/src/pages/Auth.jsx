import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
import Logo from '../components/layout/Logo';
import { Button, Field } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../lib/api';

function Shell({ title, subtitle, children }) {
  return (
    <div className="min-h-screen grid lg:grid-cols-2 noise">
      <div className="hidden lg:flex relative flex-col justify-between p-12 overflow-hidden border-r border-line">
        <div className="absolute inset-0 grid-bg [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]" />
        <div className="absolute -top-32 -left-32 w-[520px] h-[520px] rounded-full bg-up/10 blur-[110px]" />
        <div className="absolute bottom-0 right-0 w-[420px] h-[420px] rounded-full bg-violet/10 blur-[110px]" />
        <Link to="/" className="relative"><Logo /></Link>
        <div className="relative">
          <h2 className="text-4xl font-extrabold tracking-tight leading-tight">Practise with discipline.<br /><span className="text-up">Trade with clarity.</span></h2>
          <p className="text-muted mt-4 max-w-md">₹10,00,000 in virtual capital, transparent charges, and feedback on every habit that costs real traders money.</p>
        </div>
        <p className="relative text-xs text-faint">A project by <span className="text-ink font-semibold">Abdullah Shaikh</span> · Educational paper trading · No real money</p>
      </div>
      <div className="flex items-center justify-center p-6">
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-sm">
          <Link to="/" className="lg:hidden block mb-8"><Logo /></Link>
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1><p className="text-sm text-muted mt-1 mb-8">{subtitle}</p>
          {children}
        </motion.div>
      </div>
    </div>
  );
}

export function Login() {
  const { login } = useAuth(); const nav = useNavigate(); const loc = useLocation();
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [err, setErr] = useState(''); const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault(); setErr(''); setBusy(true);
    try { await login(email, password); nav(loc.state?.from || '/app', { replace: true }); } catch (x) { setErr(errorMessage(x)); } finally { setBusy(false); }
  };
  return (
    <Shell title="Welcome back" subtitle="Log in to your trading terminal">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Email"><input className="input" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></Field>
        <Field label="Password"><input className="input" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} /></Field>
        {err && <p className="text-sm text-down" role="alert">{err}</p>}
        <Button type="submit" className="w-full" loading={busy}>Log in <ArrowRight className="w-4 h-4" /></Button>
      </form>
      <div className="mt-6 rounded-lg border border-line bg-raised/60 p-3 text-xs text-muted">
        <div className="font-semibold text-ink mb-1">Abdullah’s accounts (tap to fill)</div>
        <button className="block hover:text-ink text-left" onClick={() => { setEmail('abdullah@niftynexus.in'); setPassword('Abdullah@123'); }}>Abdullah Shaikh (Trader) · abdullah@niftynexus.in / Abdullah@123</button>
        <button className="block hover:text-ink mt-0.5 text-left" onClick={() => { setEmail('abdullah.admin@niftynexus.in'); setPassword('Admin@123'); }}>Abdullah Shaikh (Admin) · abdullah.admin@niftynexus.in / Admin@123</button>
      </div>
      <p className="text-sm text-muted mt-6">New here? <Link to="/register" className="text-up font-medium">Create an account</Link></p>
    </Shell>
  );
}

export function Register() {
  const { register } = useAuth(); const nav = useNavigate();
  const [f, setF] = useState({ name: '', email: '', password: '' }); const [err, setErr] = useState(''); const [busy, setBusy] = useState(false);
  const strong = f.password.length >= 8 && /[0-9]/.test(f.password) && /[A-Za-z]/.test(f.password);
  const submit = async (e) => {
    e.preventDefault(); setErr('');
    if (f.name.trim().length < 2) return setErr('Please enter your name');
    if (!/^\S+@\S+\.\S+$/.test(f.email)) return setErr('Please enter a valid email');
    if (!strong) return setErr('Password needs at least 8 characters with letters and numbers');
    setBusy(true);
    try { await register(f.name, f.email, f.password); nav('/app', { replace: true }); } catch (x) { setErr(errorMessage(x)); } finally { setBusy(false); }
  };
  return (
    <Shell title="Create your account" subtitle="Get ₹10,00,000 in virtual capital instantly">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Full name"><input className="input" autoComplete="name" placeholder="Abdullah Shaikh" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
        <Field label="Email"><input className="input" type="email" autoComplete="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
        <Field label="Password" hint="8+ characters with letters and numbers"><input className="input" type="password" autoComplete="new-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} />
          <div className="h-1 rounded bg-raised mt-2 overflow-hidden"><div className={`h-full transition-all ${strong ? 'bg-up w-full' : f.password ? 'bg-amber w-1/2' : 'w-0'}`} /></div></Field>
        {err && <p className="text-sm text-down" role="alert">{err}</p>}
        <Button type="submit" className="w-full" loading={busy}>Create account <ArrowRight className="w-4 h-4" /></Button>
      </form>
      <p className="text-sm text-muted mt-6">Already have an account? <Link to="/login" className="text-up font-medium">Log in</Link></p>
    </Shell>
  );
}
