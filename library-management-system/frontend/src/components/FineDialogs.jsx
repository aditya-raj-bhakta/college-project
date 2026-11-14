import { useState } from 'react';
import Modal from './Modal';
import { finesApi } from '../api/services';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { formatRupees } from '../utils/format';

/** Take a payment against a fine. Part payments are fine; the balance stays open. */
export function PayDialog({ fine, onClose, onDone }) {
  return (
    <FineDialog
      fine={fine}
      title="Take a payment"
      confirm="Record payment"
      noteLabel="Note"
      notePlaceholder="Cash, UPI, card"
      noteRequired={false}
      submit={(amount, note) => finesApi.pay(fine.id, amount, note)}
      onClose={onClose}
      onDone={onDone}
    />
  );
}

/** Waive part or all of a fine. A reason is required and kept in the ledger and audit log. */
export function WaiveDialog({ fine, onClose, onDone }) {
  return (
    <FineDialog
      fine={fine}
      title="Waive a fine"
      confirm="Waive"
      noteLabel="Reason"
      notePlaceholder="Brought a replacement copy"
      noteRequired
      submit={(amount, reason) => finesApi.waive(fine.id, amount, reason)}
      onClose={onClose}
      onDone={onDone}
    />
  );
}

function FineDialog({ fine, title, confirm, noteLabel, notePlaceholder, noteRequired, submit, onClose, onDone }) {
  const [amount, setAmount] = useState(fine.balance);
  const [note, setNote] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const handle = async (e) => {
    e.preventDefault();
    setBusy(true);
    setFormError('');
    setErrors({});
    try {
      onDone(await submit(Number(amount), note));
    } catch (err) {
      setErrors(getFieldErrors(err));
      setFormError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={title}
      onClose={onClose}
      width={440}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="fine-form" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : confirm}</button>
        </>
      }
    >
      <form id="fine-form" className="form-stack" onSubmit={handle}>
        <div className="fine-summary">
          <span>{fine.memberName} · {fine.cardNumber}</span>
          <span className="cell-sub">{fine.description}</span>
          <span className="fine-summary-balance">{formatRupees(fine.balance)} <span className="cell-sub">owed of {formatRupees(fine.amount)}</span></span>
        </div>
        {formError && <div className="alert alert-error">{formError}</div>}
        <label className="field">
          <span>Amount (₹)</span>
          <input type="number" min={1} max={fine.balance} value={amount} onChange={(e) => setAmount(e.target.value)} autoFocus />
          {errors.amount && <small className="field-error">{errors.amount}</small>}
        </label>
        <label className="field">
          <span>{noteLabel}{noteRequired ? '' : ' (optional)'}</span>
          <input value={note} onChange={(e) => setNote(e.target.value)} placeholder={notePlaceholder} />
          {(errors.reason || errors.note) && <small className="field-error">{errors.reason || errors.note}</small>}
        </label>
      </form>
    </Modal>
  );
}
