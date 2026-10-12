import { Link } from 'react-router-dom';
export default function NotFound() {
  return (
    <div className="min-h-[60vh] grid place-items-center text-center p-8">
      <div><div className="num text-6xl font-bold text-faint">404</div><h1 className="text-xl font-semibold mt-3">This page isn’t on the tape</h1>
        <p className="text-sm text-muted mt-1">The page you’re looking for doesn’t exist.</p><Link to="/" className="btn-primary mt-6">Go home</Link></div>
    </div>
  );
}
