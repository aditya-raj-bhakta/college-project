import { useState } from 'react';
import { adminApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import useAsync from '../../hooks/useAsync';
import Spinner from '../../components/Spinner';
import Icon from '../../components/Icon';
import { formatDate } from '../../utils/format';

const EMPTY = { name: '', email: '', password: '', role: 'LIBRARIAN' };

export default function StaffPage() {
  const toast = useToast();
  const { data: staff, error, reload } = useAsync(() => adminApi.staff(), []);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    setFormError('');
    try {
      const created = await adminApi.createStaff(form);
      toast(`${created.name} can now log in as a ${created.role.toLowerCase()}`);
      setForm(EMPTY);
      reload();
    } catch (err) {
      setErrors(getFieldErrors(err));
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Staff</h1>
          <p className="page-meta">Librarians run the desk and the catalogue. Admins also set rules and see the audit log.</p>
        </div>
      </div>

      <div className="staff-grid">
        <section className="card">
          <div className="card-header"><h2>Accounts</h2></div>
          {!staff ? <Spinner /> : error ? <div className="alert alert-error">{error}</div> : (
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Since</th></tr></thead>
                <tbody>
                  {staff.map((s) => (
                    <tr key={s.id}>
                      <td className="cell-strong">{s.name}</td>
                      <td className="cell-sub-inline">{s.email}</td>
                      <td><span className={`status ${s.role === 'ADMIN' ? 'tone-accent' : 'tone-outline'}`}>{s.role === 'ADMIN' ? 'Admin' : 'Librarian'}</span></td>
                      <td className="cell-date">{formatDate(s.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <form className="card" onSubmit={submit} noValidate>
          <div className="card-header"><h2>Add a staff account</h2></div>
          <div className="form-stack">
            {formError && <div className="alert alert-error">{formError}</div>}
            <label className="field"><span>Name</span><input value={form.name} onChange={set('name')} />{errors.name && <small className="field-error">{errors.name}</small>}</label>
            <label className="field"><span>Email</span><input type="email" value={form.email} onChange={set('email')} />{errors.email && <small className="field-error">{errors.email}</small>}</label>
            <label className="field"><span>Temporary password</span><input type="password" value={form.password} onChange={set('password')} autoComplete="new-password" />{errors.password && <small className="field-error">{errors.password}</small>}</label>
            <label className="field">
              <span>Role</span>
              <select value={form.role} onChange={set('role')}>
                <option value="LIBRARIAN">Librarian</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>
            <button className="btn btn-primary" disabled={saving}><Icon name="plus" />{saving ? 'Creating…' : 'Create account'}</button>
          </div>
        </form>
      </div>
    </>
  );
}
