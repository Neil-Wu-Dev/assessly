import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

type Page = 'ai' | 'controls' | 'evidence' | 'assessment' | 'benchmark'
type Dataset = { id: string; name: string; description?: string; schemaJson?: string; updatedAt: string }
type Evidence = { id: string; filename: string; format: string; rowCount: number; rowsJson: string; createdAt: string }
type Control = { id: string; filename: string; format: string; title: string; fullText: string; createdAt: string }
type RuleSet = { id: string; version: number; status: string; rulesJson: string; generationSummaryJson: string; confirmedAt?: string }
type Assessment = { id: string; recordsEvaluated: number; rulesEvaluated: number; violationsDetected: number; resultJson: string; createdAt: string }
type Detail = { dataset: Dataset; evidenceFiles: Evidence[]; controlDocuments: Control[]; ruleSets: RuleSet[]; assessmentRuns: Assessment[] }
type ApiSession = { status: string; providerName?: string; baseUrl?: string; modelName?: string; expiresAt?: string; remainingSeconds: number; message: string }
type BackendState = 'checking' | 'connected' | 'offline'

const pages: { id: Page; label: string }[] = [
  { id: 'ai', label: 'AI Provider' },
  { id: 'controls', label: 'Security Controls' },
  { id: 'evidence', label: 'Evidence & Rules' },
  { id: 'assessment', label: 'Assessment' },
  { id: 'benchmark', label: 'Reliability Benchmark' },
]

function App() {
  const [backendState, setBackendState] = useState<BackendState>('checking')
  const [sessionToken, setSessionToken] = useState(localStorage.getItem('assessly-session') ?? '')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('Backend check pending')
  const [page, setPage] = useState<Page>('ai')
  const [datasets, setDatasets] = useState<Dataset[]>([])
  const [activeId, setActiveId] = useState('')
  const [detail, setDetail] = useState<Detail | null>(null)
  const [apiSession, setApiSession] = useState<ApiSession>({ status: 'DISCONNECTED', remainingSeconds: 0, message: 'AI API key is not connected.' })
  const [manualRule, setManualRule] = useState(sampleRule)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')

  const activeRule = detail?.ruleSets[0]
  const activeEvidence = detail?.evidenceFiles[0]
  const rows = useMemo(() => parseJsonArray(activeEvidence?.rowsJson).slice(0, 25), [activeEvidence])

  useEffect(() => {
    checkBackend()
    if (sessionToken) refreshDatasets(sessionToken)
  }, [])

  async function checkBackend() {
    setBackendState('checking')
    try {
      const response = await fetch(`${apiBase}/health`)
      setBackendState(response.ok ? 'connected' : 'offline')
      setMessage(response.ok ? 'Backend connected' : 'Backend unavailable')
    } catch {
      setBackendState('offline')
      setMessage('Backend unavailable')
    }
  }

  async function request(path: string, init: RequestInit = {}, token = sessionToken) {
    const response = await fetch(`${apiBase}${path}`, {
      ...init,
      headers: {
        ...(init.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { 'X-Assessly-Session': token } : {}),
        ...(init.headers ?? {}),
      },
    })
    const data = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null
    if (!response.ok) throw new Error(data?.error ?? 'Request failed')
    return data
  }

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      const data = await request(`/auth/${authMode}`, { method: 'POST', body: JSON.stringify({ email, password }) }, '')
      localStorage.setItem('assessly-session', data.sessionToken)
      setSessionToken(data.sessionToken)
      setMessage(`${authMode === 'register' ? 'Registered' : 'Signed in'} as ${data.email}`)
      await refreshDatasets(data.sessionToken)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  function signOut() {
    localStorage.removeItem('assessly-session')
    setSessionToken('')
    setDetail(null)
    setDatasets([])
    setActiveId('')
    setPage('ai')
    setMessage('Signed out')
  }

  async function refreshDatasets(token = sessionToken) {
    if (!token) return
    try {
      const data = await request('/datasets', {}, token)
      setDatasets(data)
      if (data[0]) await loadDetail(data[0].id, token)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function loadDetail(id: string, token = sessionToken) {
    const data = await request(`/datasets/${id}`, {}, token)
    setActiveId(id)
    setDetail(data)
  }

  async function createDataset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const created = await request('/datasets', { method: 'POST', body: JSON.stringify({ name: form.get('name'), description: form.get('description') }) })
      await refreshDatasets()
      await loadDetail(created.id)
      setMessage('Dataset created')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function connectApi(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const status = await request('/auth/ai-session', {
        method: 'POST',
        body: JSON.stringify({
          providerName: form.get('providerName'),
          baseUrl: form.get('baseUrl'),
          modelName: form.get('modelName'),
          apiKey: form.get('apiKey'),
        }),
      })
      setApiSession(status)
      setMessage(status.message)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function upload(path: 'controls' | 'evidence', file?: File) {
    if (!activeId || !file) return
    const body = new FormData()
    body.append('file', file)
    try {
      await request(`/datasets/${activeId}/${path}`, { method: 'POST', body })
      await loadDetail(activeId)
      setMessage(path === 'controls' ? 'Security Control uploaded' : 'Evidence uploaded')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function generateRules() {
    try {
      await request(`/datasets/${activeId}/rules/generate`, { method: 'POST' })
      await loadDetail(activeId)
      setMessage('Rule generation completed')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function saveManualRules() {
    try {
      await request(`/datasets/${activeId}/rules/manual`, { method: 'POST', body: JSON.stringify({ rulesJson: manualRule }) })
      await loadDetail(activeId)
      setMessage('Rule Set saved and confirmed')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function startAssessment() {
    if (!activeEvidence || !activeRule) return
    try {
      await request(`/datasets/${activeId}/assessments`, { method: 'POST', body: JSON.stringify({ evidenceFileId: activeEvidence.id, ruleSetId: activeRule.id }) })
      await loadDetail(activeId)
      setMessage('Assessment completed')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  if (!sessionToken) {
    return <AuthPage backendState={backendState} message={message} authMode={authMode} setAuthMode={setAuthMode} email={email} setEmail={setEmail} password={password} setPassword={setPassword} submitAuth={submitAuth} checkBackend={checkBackend} />
  }

  return (
    <div className="app-shell">
      <header className="browser-bar">
        <div className="brand-lockup"><div className="logo">A</div><div><strong>Assessly</strong><span>Cybersecurity Assessment</span></div></div>
        <nav className="top-nav" aria-label="Primary navigation">
          {pages.map((item) => <button key={item.id} className={page === item.id ? 'selected' : ''} onClick={() => setPage(item.id)}>{item.label}</button>)}
        </nav>
        <div className="header-actions"><BackendBadge state={backendState} onClick={checkBackend} /><button className="ghost" onClick={signOut}>Sign out</button></div>
      </header>

      <main className="content-frame">
        <aside className="dataset-rail">
          <form className="compact-card" onSubmit={createDataset}>
            <h2>Dataset</h2>
            <input name="name" placeholder="Dataset name" />
            <input name="description" placeholder="Optional description" />
            <button>Create</button>
          </form>
          <div className="rail-list">
            {datasets.map((dataset) => <button key={dataset.id} className={dataset.id === activeId ? 'active' : ''} onClick={() => loadDetail(dataset.id)}><strong>{dataset.name}</strong><span>{new Date(dataset.updatedAt).toLocaleDateString()}</span></button>)}
          </div>
        </aside>

        <section className="page-panel">
          <div className="status-line"><span>{message}</span><span>{detail ? detail.dataset.name : 'No dataset selected'}</span></div>
          {!detail ? <EmptyDataset /> : <PageContent page={page} detail={detail} apiSession={apiSession} rows={rows} activeRule={activeRule} activeEvidence={activeEvidence} manualRule={manualRule} setManualRule={setManualRule} connectApi={connectApi} upload={upload} generateRules={generateRules} saveManualRules={saveManualRules} startAssessment={startAssessment} />}
        </section>
      </main>
    </div>
  )
}

function AuthPage(props: { backendState: BackendState; message: string; authMode: 'login' | 'register'; setAuthMode: (mode: 'login' | 'register') => void; email: string; setEmail: (value: string) => void; password: string; setPassword: (value: string) => void; submitAuth: (event: FormEvent<HTMLFormElement>) => void; checkBackend: () => void }) {
  return <main className="auth-screen"><section className="auth-card"><div className="auth-brand"><div className="logo">A</div><div><h1>Assessly</h1><p>Translate controls into auditable machine rules.</p></div></div><div className="segmented"><button className={props.authMode === 'login' ? 'selected' : ''} onClick={() => props.setAuthMode('login')}>Login</button><button className={props.authMode === 'register' ? 'selected' : ''} onClick={() => props.setAuthMode('register')}>Register</button></div><form onSubmit={props.submitAuth}><label>Email<input value={props.email} onChange={(event) => props.setEmail(event.target.value)} placeholder="name@company.com" /></label><label>Password<input type="password" value={props.password} onChange={(event) => props.setPassword(event.target.value)} placeholder="Minimum 8 characters" /></label><button className="primary">{props.authMode === 'login' ? 'Login' : 'Create account'}</button></form><footer><BackendBadge state={props.backendState} onClick={props.checkBackend} /><span>{props.message}</span></footer></section></main>
}

function PageContent(props: { page: Page; detail: Detail; apiSession: ApiSession; rows: Record<string, unknown>[]; activeRule?: RuleSet; activeEvidence?: Evidence; manualRule: string; setManualRule: (value: string) => void; connectApi: (event: FormEvent<HTMLFormElement>) => void; upload: (path: 'controls' | 'evidence', file?: File) => void; generateRules: () => void; saveManualRules: () => void; startAssessment: () => void }) {
  if (props.page === 'ai') return <AiProviderPage apiSession={props.apiSession} connectApi={props.connectApi} />
  if (props.page === 'controls') return <ControlsPage controls={props.detail.controlDocuments} upload={props.upload} />
  if (props.page === 'evidence') return <EvidenceRulesPage detail={props.detail} rows={props.rows} activeRule={props.activeRule} manualRule={props.manualRule} setManualRule={props.setManualRule} upload={props.upload} generateRules={props.generateRules} saveManualRules={props.saveManualRules} />
  if (props.page === 'assessment') return <AssessmentPage evidence={props.activeEvidence} rule={props.activeRule} runs={props.detail.assessmentRuns} startAssessment={props.startAssessment} />
  return <BenchmarkPage />
}

function AiProviderPage({ apiSession, connectApi }: { apiSession: ApiSession; connectApi: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="view"><div className="view-heading"><p>Step 1</p><h1>AI Provider Registration</h1><span>API keys stay in memory session only.</span></div><form className="form-grid" onSubmit={connectApi}><label>Provider name<input name="providerName" defaultValue={apiSession.providerName ?? ''} placeholder="Any OpenAI-compatible provider" /></label><label>Base URL<input name="baseUrl" defaultValue={apiSession.baseUrl ?? ''} placeholder="https://provider.example.com" /></label><label>Model name<input name="modelName" defaultValue={apiSession.modelName ?? ''} placeholder="model-name" /></label><label>API key<input name="apiKey" type="password" placeholder="Never stored in database" /></label><button className="primary">Validate and Connect</button></form><div className="info-strip"><StatusPill value={apiSession.status} /><span>{apiSession.expiresAt ? `Expires at ${new Date(apiSession.expiresAt).toLocaleTimeString()}` : 'No active AI API session'}</span></div></div>
}

function ControlsPage({ controls, upload }: { controls: Control[]; upload: (path: 'controls' | 'evidence', file?: File) => void }) {
  return <div className="view"><div className="view-heading"><p>Step 2</p><h1>Upload Security Controls</h1><span>Supported: PDF, DOCX, TXT, Markdown, HTML.</span></div><UploadBox label="Upload control document" onFile={(file) => upload('controls', file)} /><div className="list-table">{controls.map((control) => <article key={control.id}><strong>{control.title || control.filename}</strong><span>{control.format}</span><p>{control.fullText.slice(0, 260)}</p></article>)}</div></div>
}

function EvidenceRulesPage(props: { detail: Detail; rows: Record<string, unknown>[]; activeRule?: RuleSet; manualRule: string; setManualRule: (value: string) => void; upload: (path: 'controls' | 'evidence', file?: File) => void; generateRules: () => void; saveManualRules: () => void }) {
  return <div className="view"><div className="view-heading"><p>Step 3</p><h1>Evidence, Control Selection, Rule Building</h1><span>Upload structured cybersecurity data, then generate or edit rule blocks.</span></div><div className="two-column"><section><UploadBox label="Upload evidence data" hint="CSV, XLSX, JSON, JSONL, XML" onFile={(file) => props.upload('evidence', file)} /><DataPreview rows={props.rows} /></section><section className="rule-workbench"><label>Security Control source<select>{props.detail.controlDocuments.map((control) => <option key={control.id}>{control.title || control.filename}</option>)}</select></label><button className="primary" onClick={props.generateRules}>Generate Rule Blocks with AI</button><RuleSummary rule={props.activeRule} /><h2>Visual Rule Builder Draft</h2><div className="builder-row"><select><option>IF</option><option>AND</option><option>OR</option><option>NOT</option></select><select><option>role</option><option>mfa_enabled</option></select><select><option>=</option><option>IN</option><option>CONTAINS</option></select><input defaultValue="admin" /></div><textarea className="code-editor" value={props.manualRule} onChange={(event) => props.setManualRule(event.target.value)} /><button onClick={props.saveManualRules}>Save Executable Rule Set</button></section></div></div>
}

function AssessmentPage({ evidence, rule, runs, startAssessment }: { evidence?: Evidence; rule?: RuleSet; runs: Assessment[]; startAssessment: () => void }) {
  return <div className="view"><div className="view-heading"><p>Step 4</p><h1>Deterministic Assessment</h1><span>No AI is called during assessment execution.</span></div><div className="action-card"><div><strong>{evidence ? evidence.filename : 'No evidence selected'}</strong><span>{rule ? `Rule Set v${rule.version} - ${rule.status}` : 'No rule set selected'}</span></div><button className="primary" disabled={!evidence || !rule} onClick={startAssessment}>Start Assessment</button></div><History runs={runs} /></div>
}

function BenchmarkPage() {
  return <div className="view"><div className="view-heading"><p>Evaluation</p><h1>AI Reliability Benchmark</h1><span>Compare generated AST and execution results against human ground truth.</span></div><div className="placeholder-grid"><article><strong>Built-in cases</strong><span>Pending backend benchmark endpoints</span></article><article><strong>Custom cases</strong><span>Control text, test data, ground truth rule, expected result</span></article><article><strong>Metrics</strong><span>Accuracy, precision, recall, false positives, false negatives</span></article></div></div>
}

function EmptyDataset() {
  return <div className="empty-dataset"><h1>No Dataset Selected</h1><p>Create a dataset in the left rail before uploading controls or evidence.</p></div>
}

function UploadBox({ label, hint, onFile }: { label: string; hint?: string; onFile: (file?: File) => void }) {
  return <label className="upload-box"><span>{label}</span>{hint && <small>{hint}</small>}<input type="file" onChange={(event) => onFile(event.target.files?.[0])} /></label>
}

function BackendBadge({ state, onClick }: { state: BackendState; onClick: () => void }) {
  const label = state === 'connected' ? 'Backend connected' : state === 'offline' ? 'Backend offline' : 'Checking backend'
  return <button className={`backend-badge ${state}`} onClick={onClick}><span />{label}</button>
}

function StatusPill({ value }: { value: string }) {
  return <span className={`status-pill ${value.toLowerCase()}`}>{value}</span>
}

function DataPreview({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <div className="empty-inline">No structured evidence uploaded.</div>
  const columns = Object.keys(rows[0])
  return <div className="table-wrap"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{columns.map((column) => <td key={column}>{String(row[column] ?? '')}</td>)}</tr>)}</tbody></table></div>
}

function RuleSummary({ rule }: { rule?: RuleSet }) {
  if (!rule) return <div className="empty-inline">No rule set generated yet.</div>
  return <div className="rule-summary"><div><StatusPill value={rule.status} /><strong>Version {rule.version}</strong></div><pre>{pretty(rule.rulesJson)}</pre></div>
}

function History({ runs }: { runs: Assessment[] }) {
  if (!runs.length) return <div className="empty-inline">No assessment runs yet.</div>
  return <div className="history-list">{runs.map((run) => <article key={run.id}><div><strong>{new Date(run.createdAt).toLocaleString()}</strong><span>{run.recordsEvaluated} records · {run.rulesEvaluated} rules</span></div><StatusPill value={run.violationsDetected ? `${run.violationsDetected} VIOLATIONS` : 'PASS'} /><pre>{pretty(run.resultJson)}</pre></article>)}</div>
}

function parseJsonArray(json?: string) {
  try { return json ? JSON.parse(json) : [] } catch { return [] }
}

function pretty(json: string) {
  try { return JSON.stringify(JSON.parse(json), null, 2) } catch { return json }
}

const sampleRule = JSON.stringify({ rules: [{ id: 'manual-rule-1', fieldsUsed: ['role', 'mfa_enabled'], sourceControl: { controlId: 'IAM-04', text: 'Administrative accounts must use MFA.' }, explanation: 'The IF condition identifies admin accounts, then requires MFA to be enabled.', ast: { type: 'if', if: { type: 'condition', field: 'role', operator: '=', value: 'admin' }, then: { type: 'condition', field: 'mfa_enabled', operator: '=', value: true } } }] }, null, 2)

export default App
