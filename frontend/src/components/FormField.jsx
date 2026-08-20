export function FormField({ label, type = 'text', value, onChange, error, ...rest }) {
  return (
    <div className="field">
      <label>{label}</label>
      <input type={type} value={value} onChange={onChange} {...rest} />
      {error && <div className="field__error">{error}</div>}
    </div>
  );
}
