import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(p) { super(p); this.state = { error: null }; }
  static getDerivedStateFromError(error) { return { error }; }
  componentDidCatch(error, info) { console.error('UI error', error, info); }
  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div className="min-h-screen grid place-items-center p-8 text-center">
        <div><h1 className="text-xl font-semibold">Something went wrong</h1><p className="text-sm text-muted mt-2 max-w-md">{String(this.state.error.message || this.state.error)}</p>
          <button className="btn-primary mt-6" onClick={() => window.location.reload()}>Reload</button></div>
      </div>
    );
  }
}
