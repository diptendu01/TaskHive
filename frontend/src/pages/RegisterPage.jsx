import React, { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { api } from '../services/api';
import { Button } from '../components/ui/Button';
import { Input, Label } from '../components/ui/Input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Alert } from '../components/ui/Alert';
import { EyeIcon, EyeOffIcon, KanbanIcon } from '../components/ui/Icons';

const RegisterPage = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login, user, isReady } = useAuth();
  const navigate = useNavigate();

  if (isReady && user) {
    return <Navigate to="/" replace />;
  }

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const data = await api.register({ name, email, password });
      if (!data.accessToken || !login(data.accessToken)) {
        setError('Received an invalid authentication token from the server.');
        return;
      }
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || 'Registration failed. Please check your details.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main
      id="main-content"
      className="min-h-screen bg-background text-foreground flex flex-col items-center justify-center px-4 py-12 relative overflow-hidden"
    >
      <div
        className="pointer-events-none absolute top-0 left-1/2 -translate-x-1/2 h-[320px] w-[640px] rounded-full bg-teal-500/[0.06] blur-3xl"
        aria-hidden="true"
      />

      <div className="relative w-full max-w-sm">
        <div className="flex items-center justify-center gap-2.5 mb-8">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-card text-accent">
            <KanbanIcon className="h-5 w-5" />
          </span>
          <span className="text-lg font-semibold tracking-tight">TaskHive</span>
        </div>

        <Card className="bg-surface border-border">
          <CardHeader className="pb-5">
            <CardTitle>Create an account</CardTitle>
            <CardDescription>Start organizing boards and collaborating in real time.</CardDescription>
          </CardHeader>

          <CardContent>
            {error && (
              <Alert onDismiss={() => setError('')} className="mb-4">
                {error}
              </Alert>
            )}

            <form onSubmit={handleRegister} className="space-y-4">
              <div>
                <Label htmlFor="register-name">Full name</Label>
                <Input
                  id="register-name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Alex Rivera"
                  required
                  maxLength={120}
                  autoComplete="name"
                />
              </div>

              <div>
                <Label htmlFor="register-email">Email</Label>
                <Input
                  id="register-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@company.com"
                  required
                  maxLength={255}
                  autoComplete="email"
                />
              </div>

              <div>
                <Label htmlFor="register-password">Password</Label>
                <div className="relative">
                  <Input
                    id="register-password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    minLength={6}
                    maxLength={128}
                    required
                    autoComplete="new-password"
                    className="pr-9"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer p-0.5 rounded"
                  >
                    {showPassword ? (
                      <EyeOffIcon className="h-4 w-4" />
                    ) : (
                      <EyeIcon className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>

              <Button type="submit" disabled={submitting} className="w-full mt-2">
                {submitting ? 'Creating account...' : 'Create Account'}
              </Button>
            </form>

            <p className="text-center text-xs text-muted-foreground mt-6">
              Already have an account?{' '}
              <Link
                to="/login"
                className="text-foreground font-medium hover:text-accent transition-colors underline underline-offset-4"
              >
                Sign in
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </main>
  );
};

export default RegisterPage;
