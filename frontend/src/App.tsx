import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

type Dataset = { id: string; name: string; description?: string; schemaJson?: string; updatedAt: string }
type Evidence = { id: string; filename: string; format: string; rowCount: number; rowsJson: string }
type Control = { id: string; filename: string; format: string; title: string; fullText: string }
type RuleSet = { id: string; version: number; status: string; rulesJson: string; generationSummaryJson: string; confirmedAt?: string }
type Assessment = { id: string; recordsEvaluated: number; rulesEvaluated: number; violationsDetected: number; resultJson: string; createdAt: string }
type Detail = { dataset: Dataset; evidenceFiles: Evidence[]; controlDocuments: Control[]; ruleSets: RuleSet[]; assessmentRuns: Assessment[] }
type ApiSession = { status: string; expiresAt?: string; remainingSeconds: number; message: string }

function App() {
  const [sessionToken, setSessionToken] = useState(localStorage.getItem('assessly-session') ?? '')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('Ready')
  const [datasets, setDatasets] = useState<Dataset[]>([])
  const [activeId, setActiveId] = useState('')
  const [detail, setDetail] = useState<Detail | null>(null)
  const [apiSession, setApiSession] = useState<ApiSession>({ status: 'DISCONNECTED', remainingSeconds: 0, message: 'DeepSeek API key is not connected.' })
  const [manualRule, setManualRule] = useState(sampleRule)

  const activeRule = detail?.ruleSets[0]
  const activeEvidence = detail?.evidenceFiles[0]
  const rows = useMemo(() => parseJsonArray(activeEvidence?.rowsJson).slice(0, 20), [activeEvidence])

  async function request(path: string, init: RequestInit = {}) {
    const response = await fetch(`${apiBase}${path}`, {
      ...init,
      headers: {
        ...(init.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...(sessionToken ? { 'X-Assessly-Session': sessionToken } : {}),
        ...(init.headers ?? {}),
      },
    })
    const data = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null
    if (!response.ok) throw new Error(data?.error ?? 'Something went wrong')
    return data
  }

  async function auth(mode: 'register' | 'login') {
    try {
      const data = await request(`/auth/${mode}`, { method: 'POST', body: JSON.stringify({ email, password }) })
      localStorage.setItem('assessly-session', data.sessionToken)
      setSessionToken(data.sessionToken)
      setMessage(`${mode === 'register' ? 'Registered' : 'Signed in'} as ${data.email}`)
      await refreshDatasets(data.sessionToken)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function refreshDatasets(token = sessionToken) {
    if (!token) return
    const response = await fetch(`${apiBase}/datasets`, { headers: { 'X-Assessly-Session': token } })
    const data = await response.json()
    setDatasets(data)
    if (data[0]) await loadDetail(data[0].id, token)
  }

  async function loadDetail(id: string, token = sessionToken) {
    const response = await fetch(`${apiBase}/datasets/${id}`, { headers: { 'X-Assessly-Session': token } })
    const data = await response.json()
    setActiveId(id)
    setDetail(data)
  }

  async function createDataset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const created = await request('/datasets', { method: 'POST', body: JSON.stringify({ name: form.get('name'), description: form.get('description') }) })
      setMessage('Dataset created')
      await loadDetail(created.id)
      await refreshDatasets()
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function upload(path: string, file?: File) {
    if (!activeId || !file) return
    const body = new FormData()
    body.append('file', file)
    try {
      await request(`/datasets/${activeId}/${path}`, { method: 'POST', body })
      setMessage('Upload complete')
      await loadDetail(activeId)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function connectApi(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const apiKey = String(new FormData(event.currentTarget).get('apiKey'))
    try {
      const status = await request('/auth/deepseek', { method: 'POST', body: JSON.stringify({ apiKey }) })
      setApiSession(status)
      setMessage(status.message)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function generateRules() {
    try {
      await request(`/datasets/${activeId}/rules/generate`, { method: 'POST' })
      setMessage('Rule generation completed')
      await loadDetail(activeId)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function saveManualRules() {
    try {
      await request(`/datasets/${activeId}/rules/manual`, { method: 'POST', body: JSON.stringify({ rulesJson: manualRule }) })
      setMessage('Rule Set saved and confirmed')
      await loadDetail(activeId)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function startAssessment() {
    if (!activeEvidence || !activeRule) return
    try {
      await request(`/datasets/${activeId}/assessments`, { method: 'POST', body: JSON.stringify({ evidenceFileId: activeEvidence.id, ruleSetId: activeRule.id }) })
      setMessage('Assessment completed')
      await loadDetail(activeId)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand"><span className="mark">A</span><div><h1>Assessly</h1><p>Cybersecurity assessment PoC</p></div></div>
        <section className="panel"><h2>Account</h2><input placeholder="Email" value={email} onChange={(event) => setEmail(event.target.value)} /><input placeholder="Password, min 8 chars" type="password" value={password} onChange={(event) => setPassword(event.target.value)} /><div className="row"><button onClick={() => auth('login')}>Sign in</button><button className="secondary" onClick={() => auth('register')}>Register</button></div></section>
        <form className="panel" onSubmit={createDataset}><h2>Dataset</h2><input name="name" placeholder="Dataset name" /><textarea name="description" placeholder="Description" /><button>Create Dataset</button></form>
        <nav className="dataset-list">{datasets.map((dataset) => <button key={dataset.id} className={dataset.id === activeId ? 'active' : ''} onClick={() => loadDetail(dataset.id)}>{dataset.name}</button>)}</nav>
      </aside>
      <section className="workspace">
        <header className="topbar"><div><p className="eyebrow">Status</p><strong>{message}</strong></div><form className="api-connect" onSubmit={connectApi}><span className={`pill ${apiSession.status.toLowerCase()}`}>{apiSession.status}</span><span>{apiSession.expiresAt ? `Expires ${new Date(apiSession.expiresAt).toLocaleTimeString()}` : 'No API session'}</span><input name="apiKey" type="password" placeholder="DeepSeek API Key" /><button>Connect</button></form></header>
        {!detail ? <div className="empty">Create or select a Dataset to begin.</div> : <div className="grid">
          <section className="panel wide"><div className="section-title"><div><p className="eyebrow">Dataset</p><h2>{detail.dataset.name}</h2></div><span className="pill">Schema tracked</span></div><div className="upload-row"><label>Evidence CSV, XLSX, JSON, JSONL, XML<input type="file" onChange={(event) => upload('evidence', event.target.files?.[0])} /></label><label>Controls PDF, DOCX, TXT, MD, HTML<input type="file" onChange={(event) => upload('controls', event.target.files?.[0])} /></label></div><DataPreview rows={rows} /></section>
          <section className="panel"><h2>Rule Generation</h2><p className="muted">AI translates controls into constrained AST. Final PASS/FAIL remains deterministic.</p><button onClick={generateRules}>Generate Rules</button><RuleSummary rule={activeRule} /></section>
          <section className="panel"><h2>Visual Rule Builder</h2><div className="builder"><select><option>role</option><option>mfa_enabled</option></select><select><option>=</option><option>IN</option><option>CONTAINS</option></select><input defaultValue="admin" /><select><option>THEN</option><option>AND</option><option>OR</option></select><select><option>mfa_enabled</option><option>role</option></select><select><option>=</option></select><select><option>true</option><option>false</option></select></div><textarea className="code" value={manualRule} onChange={(event) => setManualRule(event.target.value)} /><button onClick={saveManualRules}>Save Confirmed Rule Set</button></section>
          <section className="panel wide"><div className="section-title"><h2>Assessment</h2><button onClick={startAssessment} disabled={!activeRule || !activeEvidence}>Start Assessment</button></div><History runs={detail.assessmentRuns} /></section>
        </div>}
      </section>
    </main>
  )
}

function DataPreview({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <p className="muted">No structured evidence uploaded yet.</p>
  const columns = Object.keys(rows[0])
  return <div className="table-wrap"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{columns.map((column) => <td key={column}>{String(row[column] ?? '')}</td>)}</tr>)}</tbody></table></div>
}

function RuleSummary({ rule }: { rule?: RuleSet }) {
  if (!rule) return <p className="muted">No Rule Set yet.</p>
  return <div className="rule"><span className={`pill ${rule.status.toLowerCase()}`}>{rule.status}</span><strong>Version {rule.version}</strong><pre>{pretty(rule.rulesJson)}</pre></div>
}

function History({ runs }: { runs: Assessment[] }) {
  if (!runs.length) return <p className="muted">No assessment runs yet.</p>
  return <div className="history">{runs.map((run) => <article key={run.id}><strong>{new Date(run.createdAt).toLocaleString()}</strong><span>{run.recordsEvaluated} records</span><span>{run.rulesEvaluated} rules</span><span className={run.violationsDetected ? 'danger' : 'ok'}>{run.violationsDetected} violations</span><pre>{pretty(run.resultJson)}</pre></article>)}</div>
}

function parseJsonArray(json?: string) {
  try { return json ? JSON.parse(json) : [] } catch { return [] }
}

function pretty(json: string) {
  try { return JSON.stringify(JSON.parse(json), null, 2) } catch { return json }
}

const sampleRule = JSON.stringify({ rules: [{ id: 'manual-rule-1', fieldsUsed: ['role', 'mfa_enabled'], sourceControl: { controlId: 'IAM-04', text: 'Administrative accounts must use MFA.' }, explanation: 'The IF condition identifies admin accounts, then requires MFA to be enabled.', ast: { type: 'if', if: { type: 'condition', field: 'role', operator: '=', value: 'admin' }, then: { type: 'condition', field: 'mfa_enabled', operator: '=', value: true } } }] }, null, 2)

export default App
