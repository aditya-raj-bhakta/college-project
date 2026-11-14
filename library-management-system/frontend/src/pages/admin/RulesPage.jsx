import { useState } from 'react';
import { adminApi } from '../../api/services';
import { getErrorMessage, getFieldErrors } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import useAsync from '../../hooks/useAsync';
import Spinner from '../../components/Spinner';
import Icon from '../../components/Icon';
import { formatDateTime } from '../../utils/format';

const TYPE_FIELDS = [
  { key: 'maxLoans', label: 'Books at once', suffix: '' },
  { key: 'loanDays', label: 'Loan period', suffix: 'days' },
  { key: 'maxRenewals', label: 'Renewals', suffix: '' },
  { key: 'finePerDay', label: 'Fine per day', suffix: '₹' },
  { key: 'fineBlockThreshold', label: 'Block borrowing over', suffix: '₹' },
  { key: 'maxHolds', label: 'Holds at once', suffix: '' },
];

export default function RulesPage() {
  const { data, error, setData } = useAsync(() => adminApi.settings(), []);
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <Spinner />;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Rules</h1>
          <p className="page-meta">Changes apply to new loans, renewals and fines from now on. Every change is in the audit log.</p>
        </div>
      </div>

      <div className="rules-grid">
        {data.memberTypes.map((t, i) => (
          <MemberTypeCard key={t.id} type={t} index={i} onSaved={(saved) =>
            setData((d) => ({ ...d, memberTypes: d.memberTypes.map((x) => (x.id === saved.id ? saved : x)) }))} />
        ))}
      </div>

      <div className="rules-bottom">
        <SettingsCard settings={data} onSaved={setData} />
        <JobCard />
      </div>
    </>
  );
}

function MemberTypeCard({ type, index, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(type);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const dirty = TYPE_FIELDS.some((f) => Number(form[f.key]) !== type[f.key]) || form.name !== type.name;

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    try {
      const body = { name: form.name, ...Object.fromEntries(TYPE_FIELDS.map((f) => [f.key, Number(form[f.key])])) };
      const saved = await adminApi.updateMemberType(type.id, body);
      onSaved(saved);
      setForm(saved);
      toast(`${saved.name} rules saved`);
    } catch (err) {
      setErrors(getFieldErrors(err));
      toast(getErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="card rule-card" style={{ '--i': index }} onSubmit={save}>
      <div className="card-header">
        <input className="rule-name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} aria-label="Type name" />
        <span className="mono cell-sub">{type.code}</span>
      </div>
      <div className="rule-fields">
        {TYPE_FIELDS.map((f) => (
          <label key={f.key} className="field">
            <span>{f.label}</span>
            <div className={`affix ${f.suffix === '₹' ? 'affix-pre' : ''}`}>
              {f.suffix === '₹' && <span className="affix-text">₹</span>}
              <input type="number" min={0} value={form[f.key]} onChange={(e) => setForm({ ...form, [f.key]: e.target.value })} />
              {f.suffix && f.suffix !== '₹' && <span className="affix-text">{f.suffix}</span>}
            </div>
            {errors[f.key] && <small className="field-error">{errors[f.key]}</small>}
          </label>
        ))}
      </div>
      <div className="rule-actions">
        {dirty && <button type="button" className="btn btn-ghost btn-sm" onClick={() => setForm(type)}>Reset</button>}
        <button className="btn btn-secondary btn-sm" disabled={!dirty || saving}>{saving ? 'Saving…' : 'Save'}</button>
      </div>
    </form>
  );
}

function SettingsCard({ settings, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(settings);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    try {
      const saved = await adminApi.updateSettings({
        holdPickupDays: Number(form.holdPickupDays),
        lostItemFee: Number(form.lostItemFee),
        reminderDaysBefore: Number(form.reminderDaysBefore),
        defaultMemberTypeId: Number(form.defaultMemberTypeId),
      });
      onSaved(saved);
      toast('Library settings saved');
    } catch (err) {
      setErrors(getFieldErrors(err));
      toast(getErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="card" onSubmit={save}>
      <div className="card-header"><h2>Library settings</h2></div>
      <div className="form-grid">
        <label className="field">
          <span>Hold pickup window</span>
          <div className="affix"><input type="number" min={1} value={form.holdPickupDays} onChange={set('holdPickupDays')} /><span className="affix-text">days</span></div>
          {errors.holdPickupDays && <small className="field-error">{errors.holdPickupDays}</small>}
        </label>
        <label className="field">
          <span>Lost item fee</span>
          <div className="affix affix-pre"><span className="affix-text">₹</span><input type="number" min={0} value={form.lostItemFee} onChange={set('lostItemFee')} /></div>
          {errors.lostItemFee && <small className="field-error">{errors.lostItemFee}</small>}
        </label>
        <label className="field">
          <span>Due-date reminder</span>
          <div className="affix"><input type="number" min={0} value={form.reminderDaysBefore} onChange={set('reminderDaysBefore')} /><span className="affix-text">days before</span></div>
          {errors.reminderDaysBefore && <small className="field-error">{errors.reminderDaysBefore}</small>}
        </label>
        <label className="field">
          <span>New members join as</span>
          <select value={form.defaultMemberTypeId} onChange={set('defaultMemberTypeId')}>
            {settings.memberTypes.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </label>
      </div>
      <div className="rule-actions">
        <button className="btn btn-secondary btn-sm" disabled={saving}>{saving ? 'Saving…' : 'Save settings'}</button>
      </div>
    </form>
  );
}

function JobCard() {
  const toast = useToast();
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const run = async () => {
    setBusy(true);
    try {
      setResult(await adminApi.runDailyJobs());
    } catch (err) {
      toast(getErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };
  return (
    <section className="card">
      <div className="card-header"><h2>Daily job</h2></div>
      <p className="card-note">Runs every morning at 08:00: expires uncollected holds, sends due-date reminders and overdue notices. Each reminder goes out once per loan, so running it again is safe.</p>
      <button className="btn btn-secondary" disabled={busy} onClick={run}><Icon name="play" size={14} />{busy ? 'Running…' : 'Run now'}</button>
      {result && (
        <dl className="patron-facts job-result">
          <div><dt>Holds expired</dt><dd>{result.holdsExpired}</dd></div>
          <div><dt>Reminders</dt><dd>{result.dueSoonReminders}</dd></div>
          <div><dt>Overdue notices</dt><dd>{result.overdueNotices}</dd></div>
          <div className="job-time"><dt>Ran</dt><dd>{formatDateTime(result.ranAt)}</dd></div>
        </dl>
      )}
    </section>
  );
}
