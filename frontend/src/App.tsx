import { useEffect, useMemo, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import './App.css'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

type Page = 'ai' | 'datasets' | 'controls' | 'evidence' | 'rules' | 'assessment' | 'history' | 'benchmark' | 'api'
type BackendState = 'checking' | 'connected' | 'offline'
type Dataset = { id: string; name: string; description?: string; schemaJson?: string; createdAt: string; updatedAt: string }
type Evidence = { id: string; filename: string; format: string; rowCount: number; columnsJson: string; rowsJson: string; createdAt: string }
type Control = { id: string; filename: string; format: string; title: string; fullText: string; structureJson: string; createdAt: string }
type RuleSet = { id: string; datasetId: string; version: number; status: string; rulesJson: string; generationSummaryJson: string; createdAt: string; confirmedAt?: string }
type Assessment = { id: string; datasetId: string; ruleSetId: string; evidenceFileId: string; status: string; recordsEvaluated: number; rulesEvaluated: number; violationsDetected: number; resultJson: string; createdAt: string }
type Detail = { dataset: Dataset; evidenceFiles: Evidence[]; controlDocuments: Control[]; ruleSets: RuleSet[]; assessmentRuns: Assessment[] }
type ApiSession = { status: string; providerName?: string; baseUrl?: string; modelName?: string; expiresAt?: string; remainingSeconds: number; message: string }
type BenchmarkResult = { structurallyEquivalent: boolean; executionEquivalent: boolean; totalCases: number; correctCases: number; accuracy: number; generatedExecutionJson: string; groundTruthExecutionJson: string; differences: { type: string; message: string }[] }
type BenchmarkCase = { id: string; name: string; controlText: string; testDataJson: string; groundTruthRuleJson: string; expectedResultJson: string }

const navItems: { id: Page; label: string }[] = [
  { id: 'ai', label: 'AI Provider' },
  { id: 'datasets', label: 'Datasets' },
  { id: 'controls', label: 'Controls' },
  { id: 'evidence', label: 'Evidence Data' },
  { id: 'rules', label: 'Rule Builder' },
  { id: 'assessment', label: 'Assessment' },
  { id: 'history', label: 'History' },
  { id: 'benchmark', label: 'Benchmark' },
  { id: 'api', label: 'API Coverage' },
]

function App() {
  const [backendState, setBackendState] = useState<BackendState>('checking')
  const [sessionToken, setSessionToken] = useState(localStorage.getItem('assessly-session') ?? '')
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('Checking backend')
  const [page, setPage] = useState<Page>('ai')
  const [datasets, setDatasets] = useState<Dataset[]>([])
  const [activeId, setActiveId] = useState('')
  const [detail, setDetail] = useState<Detail | null>(null)
  const [apiSession, setApiSession] = useState<ApiSession>({ status: 'DISCONNECTED', remainingSeconds: 0, message: 'AI API key is not connected.' })
  const [manualRule, setManualRule] = useState(sampleRule)
  const [selectedEvidenceId, setSelectedEvidenceId] = useState('')
  const [selectedRuleSetId, setSelectedRuleSetId] = useState('')
  const [benchmarkResult, setBenchmarkResult] = useState<BenchmarkResult | null>(null)
  const [benchmarkCases, setBenchmarkCases] = useState<BenchmarkCase[]>([])

  const rows = useMemo(() => parseJsonArray(detail?.evidenceFiles.find((file) => file.id === selectedEvidenceId)?.rowsJson ?? detail?.evidenceFiles[0]?.rowsJson).slice(0, 50), [detail, selectedEvidenceId])

  useEffect(() => {
    void checkBackend()
    if (sessionToken) void bootstrap(sessionToken)
  }, [])

  useEffect(() => {
    if (detail?.evidenceFiles[0] && !selectedEvidenceId) setSelectedEvidenceId(detail.evidenceFiles[0].id)
    if (detail?.ruleSets[0] && !selectedRuleSetId) setSelectedRuleSetId(detail.ruleSets[0].id)
  }, [detail, selectedEvidenceId, selectedRuleSetId])

  async function checkBackend() {
    setBackendState('checking')
    try {
      const response = await fetch(`${apiBase}/health`)
      setBackendState(response.ok ? 'connected' : 'offline')
      setMessage(response.ok ? 'Backend connected' : 'Backend offline')
    } catch {
      setBackendState('offline')
      setMessage('Backend offline')
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

  async function bootstrap(token = sessionToken) {
    await refreshDatasets(token)
    await refreshBenchmarkCases(token)
    try {
      const status = await request('/auth/ai-session', {}, token)
      setApiSession(status)
    } catch {
      setApiSession({ status: 'DISCONNECTED', remainingSeconds: 0, message: 'AI API key is not connected.' })
    }
  }

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      const data = await request(`/auth/${authMode}`, { method: 'POST', body: JSON.stringify({ email, password }) }, '')
      localStorage.setItem('assessly-session', data.sessionToken)
      setSessionToken(data.sessionToken)
      setPage('ai')
      setMessage(`${authMode === 'register' ? 'Registered' : 'Logged in'} as ${data.email}`)
      await bootstrap(data.sessionToken)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  function logout() {
    localStorage.removeItem('assessly-session')
    setSessionToken('')
    setDetail(null)
    setDatasets([])
    setActiveId('')
    setPage('ai')
    setMessage('Logged out')
  }

  async function refreshDatasets(token = sessionToken) {
    try {
      const data: Dataset[] = await request('/datasets', {}, token)
      setDatasets(data)
      if (data.length && !activeId) await loadDetail(data[0].id, token)
      if (!data.length) {
        setDetail(null)
        setActiveId('')
      }
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function loadDetail(id: string, token = sessionToken) {
    try {
      const data: Detail = await request(`/datasets/${id}`, {}, token)
      setActiveId(id)
      setDetail(data)
      setSelectedEvidenceId(data.evidenceFiles[0]?.id ?? '')
      setSelectedRuleSetId(data.ruleSets[0]?.id ?? '')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function createDataset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const created: Dataset = await request('/datasets', { method: 'POST', body: JSON.stringify({ name: form.get('name'), description: form.get('description') }) })
      await refreshDatasets()
      await loadDetail(created.id)
      setMessage('Dataset created')
      event.currentTarget.reset()
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function deleteDataset(id: string) {
    try {
      await request(`/datasets/${id}`, { method: 'DELETE' })
      setMessage('Dataset deleted')
      setActiveId('')
      setDetail(null)
      await refreshDatasets()
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
        body: JSON.stringify({ providerName: form.get('providerName'), baseUrl: form.get('baseUrl'), modelName: form.get('modelName'), apiKey: form.get('apiKey') }),
      })
      setApiSession(status)
      setMessage(status.message)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function upload(kind: 'controls' | 'evidence', file?: File) {
    if (!detail || !file) return
    const body = new FormData()
    body.append('file', file)
    try {
      await request(`/datasets/${detail.dataset.id}/${kind}`, { method: 'POST', body })
      await loadDetail(detail.dataset.id)
      setMessage(kind === 'controls' ? 'Security Control uploaded' : 'Evidence uploaded')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function deleteEvidence(id: string) {
    if (!detail) return
    try {
      await request(`/datasets/${detail.dataset.id}/evidence/${id}`, { method: 'DELETE' })
      await loadDetail(detail.dataset.id)
      setMessage('Evidence file deleted')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function generateRules() {
    if (!detail) return
    try {
      await request(`/datasets/${detail.dataset.id}/rules/generate`, { method: 'POST' })
      await loadDetail(detail.dataset.id)
      setMessage('AI rule generation completed')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function saveManualRules() {
    if (!detail) return
    try {
      await request(`/datasets/${detail.dataset.id}/rules/manual`, { method: 'POST', body: JSON.stringify({ rulesJson: manualRule }) })
      await loadDetail(detail.dataset.id)
      setMessage('Manual Rule Set saved')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function confirmRuleSet(id: string) {
    if (!detail) return
    try {
      await request(`/datasets/${detail.dataset.id}/rules/${id}/confirm`, { method: 'POST' })
      await loadDetail(detail.dataset.id)
      setMessage('Rule Set confirmed')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function startAssessment() {
    if (!detail || !selectedEvidenceId || !selectedRuleSetId) return
    try {
      await request(`/datasets/${detail.dataset.id}/assessments`, { method: 'POST', body: JSON.stringify({ evidenceFileId: selectedEvidenceId, ruleSetId: selectedRuleSetId }) })
      await loadDetail(detail.dataset.id)
      setMessage('Assessment completed')
      setPage('history')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function refreshBenchmarkCases(token = sessionToken) {
    try {
      const cases: BenchmarkCase[] = await request('/benchmarks/cases', {}, token)
      setBenchmarkCases(cases)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function runBenchmark(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const result = await request('/benchmarks/run', {
        method: 'POST',
        body: JSON.stringify({
          controlText: form.get('controlText'),
          testDataJson: form.get('testDataJson'),
          groundTruthRuleJson: form.get('groundTruthRuleJson'),
          expectedResultJson: form.get('expectedResultJson'),
          aiGeneratedRuleJson: form.get('aiGeneratedRuleJson'),
        }),
      })
      setBenchmarkResult(result)
      setMessage(result.executionEquivalent ? 'Benchmark passed by execution result' : 'Benchmark found reliability differences')
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  const currentPageLabel = navItems.find((item) => item.id === page)?.label ?? 'Assessly'

  if (!sessionToken) {
    return <AuthScreen authMode={authMode} setAuthMode={setAuthMode} email={email} setEmail={setEmail} password={password} setPassword={setPassword} submitAuth={submitAuth} backendState={backendState} checkBackend={checkBackend} message={message} />
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <div className="brand"><span>A</span><div><strong>Assessly</strong><small>Cybersecurity Assessment</small></div></div>
        <nav className="nav-tabs">
          {navItems.map((item) => <button key={item.id} className={page === item.id ? 'active' : ''} onClick={() => setPage(item.id)}>{item.label}</button>)}
        </nav>
        <div className="current-page"><span>Current Page</span><strong>{currentPageLabel}</strong></div>
        <div className="top-actions"><BackendBadge state={backendState} onClick={checkBackend} /><button className="text-button" onClick={logout}>Log out</button></div>
      </header>

      <main className="page-shell">
        <div className="notice-bar"><span>{message}</span><span>{detail ? `Dataset: ${detail.dataset.name}` : 'No active dataset'}</span></div>
        {page === 'ai' && <AiProviderPage apiSession={apiSession} connectApi={connectApi} />}
        {page === 'datasets' && <DatasetsPage datasets={datasets} activeId={activeId} detail={detail} createDataset={createDataset} loadDetail={loadDetail} deleteDataset={deleteDataset} />}
        {page === 'controls' && <RequireDataset detail={detail}><ControlsPage detail={detail!} upload={upload} /></RequireDataset>}
        {page === 'evidence' && <RequireDataset detail={detail}><EvidencePage detail={detail!} rows={rows} selectedEvidenceId={selectedEvidenceId} setSelectedEvidenceId={setSelectedEvidenceId} upload={upload} deleteEvidence={deleteEvidence} /></RequireDataset>}
        {page === 'rules' && <RequireDataset detail={detail}><RulesPage detail={detail!} apiSession={apiSession} manualRule={manualRule} setManualRule={setManualRule} generateRules={generateRules} saveManualRules={saveManualRules} confirmRuleSet={confirmRuleSet} /></RequireDataset>}
        {page === 'assessment' && <RequireDataset detail={detail}><AssessmentPage detail={detail!} selectedEvidenceId={selectedEvidenceId} setSelectedEvidenceId={setSelectedEvidenceId} selectedRuleSetId={selectedRuleSetId} setSelectedRuleSetId={setSelectedRuleSetId} startAssessment={startAssessment} /></RequireDataset>}
        {page === 'history' && <RequireDataset detail={detail}><HistoryPage runs={detail!.assessmentRuns} /></RequireDataset>}
        {page === 'benchmark' && <BenchmarkPage cases={benchmarkCases} refreshCases={refreshBenchmarkCases} runBenchmark={runBenchmark} result={benchmarkResult} />}
        {page === 'api' && <ApiCoveragePage />}
      </main>
    </div>
  )
}

function AuthScreen(props: { authMode: 'login' | 'register'; setAuthMode: (value: 'login' | 'register') => void; email: string; setEmail: (value: string) => void; password: string; setPassword: (value: string) => void; submitAuth: (event: FormEvent<HTMLFormElement>) => void; backendState: BackendState; checkBackend: () => void; message: string }) {
  return <main className="auth-screen"><section className="auth-panel"><div className="auth-brand"><span>A</span><div><h1>Assessly</h1><p>Controlled AI rule translation. Deterministic security assessment.</p></div></div><div className="mode-switch"><button className={props.authMode === 'login' ? 'active' : ''} onClick={() => props.setAuthMode('login')}>Login</button><button className={props.authMode === 'register' ? 'active' : ''} onClick={() => props.setAuthMode('register')}>Register</button></div><form onSubmit={props.submitAuth}><label>Email<input value={props.email} onChange={(event) => props.setEmail(event.target.value)} placeholder="name@company.com" /></label><label>Password<input type="password" value={props.password} onChange={(event) => props.setPassword(event.target.value)} placeholder="Minimum 8 characters" /></label><button className="primary">{props.authMode === 'login' ? 'Login' : 'Create account'}</button></form><footer><BackendBadge state={props.backendState} onClick={props.checkBackend} /><span>{props.message}</span></footer></section></main>
}

function AiProviderPage({ apiSession, connectApi }: { apiSession: ApiSession; connectApi: (event: FormEvent<HTMLFormElement>) => void }) {
  return <section className="page"><PageHeader eyebrow="Page 1" title="AI Provider Registration" subtitle="Configure any OpenAI-compatible provider. API key is validated and kept only in the temporary server session." /><form className="card form-grid" onSubmit={connectApi}><label>Provider name<input name="providerName" defaultValue={apiSession.providerName ?? ''} placeholder="Provider name" /></label><label>Base URL<input name="baseUrl" defaultValue={apiSession.baseUrl ?? ''} placeholder="https://provider.example.com" /></label><label>Model name<input name="modelName" defaultValue={apiSession.modelName ?? ''} placeholder="model-name" /></label><label>API key<input name="apiKey" type="password" placeholder="Never stored in database" /></label><button className="primary">Validate and Connect</button></form><div className="status-card"><StatusPill value={apiSession.status} /><span>{apiSession.expiresAt ? `Expires at ${new Date(apiSession.expiresAt).toLocaleTimeString()}` : 'No active AI API session'}</span><span>{apiSession.providerName ? `${apiSession.providerName} / ${apiSession.modelName}` : 'Provider not configured'}</span></div></section>
}

function DatasetsPage(props: { datasets: Dataset[]; activeId: string; detail: Detail | null; createDataset: (event: FormEvent<HTMLFormElement>) => void; loadDetail: (id: string) => void; deleteDataset: (id: string) => void }) {
  return <section className="page"><PageHeader eyebrow="Workspace" title="Dataset Manager" subtitle="Create, select, inspect, and delete assessment datasets." /><div className="split"><form className="card form-stack" onSubmit={props.createDataset}><h2>Create Dataset</h2><label>Name<input name="name" placeholder="Cloud IAM Review" /></label><label>Description<input name="description" placeholder="Optional" /></label><button className="primary">Create Dataset</button></form><div className="card"><h2>Datasets</h2>{props.datasets.length ? <div className="records">{props.datasets.map((dataset) => <article key={dataset.id} className={dataset.id === props.activeId ? 'selected-record' : ''}><button onClick={() => props.loadDetail(dataset.id)}><strong>{dataset.name}</strong><span>{dataset.description || 'No description'}</span></button><button className="danger-button" onClick={() => props.deleteDataset(dataset.id)}>Delete</button></article>)}</div> : <EmptyState title="No datasets yet" body="Create a Dataset first. Controls, evidence, rules, and assessment history are organized under a selected Dataset." />}</div></div>{props.detail && <div className="card metrics"><Metric label="Evidence files" value={props.detail.evidenceFiles.length} /><Metric label="Control docs" value={props.detail.controlDocuments.length} /><Metric label="Rule sets" value={props.detail.ruleSets.length} /><Metric label="Assessment runs" value={props.detail.assessmentRuns.length} /></div>}</section>
}

function ControlsPage({ detail, upload }: { detail: Detail; upload: (kind: 'controls' | 'evidence', file?: File) => void }) {
  return <section className="page"><PageHeader eyebrow="Page 2" title="Security Control Upload" subtitle="Upload natural-language rule documents. Full original text remains preserved for traceability." /><UploadCard title="Upload Control Document" description="Supported formats: PDF, DOCX, TXT, Markdown, HTML." onFile={(file) => upload('controls', file)} /><div className="card"><h2>Uploaded Controls</h2>{detail.controlDocuments.length ? <div className="records vertical">{detail.controlDocuments.map((control) => <article key={control.id}><strong>{control.title || control.filename}</strong><span>{control.filename} · {control.format} · {new Date(control.createdAt).toLocaleString()}</span><p>{control.fullText.slice(0, 420)}</p></article>)}</div> : <EmptyState title="No control documents" body="Upload PDF, DOCX, TXT, Markdown, or HTML Security Control documents here before generating rules." />}</div></section>
}

function EvidencePage(props: { detail: Detail; rows: Record<string, unknown>[]; selectedEvidenceId: string; setSelectedEvidenceId: (value: string) => void; upload: (kind: 'controls' | 'evidence', file?: File) => void; deleteEvidence: (id: string) => void }) {
  return <section className="page"><PageHeader eyebrow="Page 3" title="Cybersecurity Evidence Data" subtitle="Upload structured evidence only. Unsupported or unstructured files are rejected by the backend parser." /><UploadCard title="Upload Evidence" description="Supported formats: CSV, XLSX, JSON, JSONL, XML." onFile={(file) => props.upload('evidence', file)} /><div className="split"><div className="card"><h2>Evidence Files</h2>{props.detail.evidenceFiles.length ? <div className="records vertical">{props.detail.evidenceFiles.map((file) => <article key={file.id} className={file.id === props.selectedEvidenceId ? 'selected-record' : ''}><button onClick={() => props.setSelectedEvidenceId(file.id)}><strong>{file.filename}</strong><span>{file.format} · {file.rowCount} rows · {new Date(file.createdAt).toLocaleString()}</span></button><button className="danger-button" onClick={() => props.deleteEvidence(file.id)}>Delete</button></article>)}</div> : <EmptyState title="No evidence files" body="Upload CSV, XLSX, JSON, JSONL, or XML evidence. Files that cannot become columns and rows are rejected." />}</div><div className="card"><h2>Table Preview</h2><DataPreview rows={props.rows} /></div></div></section>
}

function RulesPage(props: { detail: Detail; apiSession: ApiSession; manualRule: string; setManualRule: (value: string) => void; generateRules: () => void; saveManualRules: () => void; confirmRuleSet: (id: string) => void }) {
  const canGenerate = props.detail.controlDocuments.length > 0 && props.detail.evidenceFiles.length > 0 && props.apiSession.status === 'CONNECTED'
  return <section className="page"><PageHeader eyebrow="Page 4" title="AI Rule Blocks and Visual Builder" subtitle="Select uploaded controls and evidence context, generate constrained rule AST, review, edit, and confirm." /><div className="rule-layout"><div className="card form-stack"><h2>Generation Inputs</h2><label>AI Session<StatusPill value={props.apiSession.status} /></label><label>Security Control{props.detail.controlDocuments.length ? <select>{props.detail.controlDocuments.map((control) => <option key={control.id}>{control.title || control.filename}</option>)}</select> : <EmptyState title="No controls available" body="Upload Security Control documents before asking AI to generate rule blocks." />}</label><label>Evidence Schema{props.detail.evidenceFiles.length ? <select>{props.detail.evidenceFiles.map((file) => <option key={file.id}>{file.filename}</option>)}</select> : <EmptyState title="No evidence available" body="Upload structured evidence so rule generation can compare controls against real fields." />}</label><button className="primary" disabled={!canGenerate} onClick={props.generateRules}>Generate Rule Blocks</button><div className="builder-row"><select><option>IF</option><option>AND</option><option>OR</option><option>NOT</option></select><select><option>role</option><option>mfa_enabled</option></select><select><option>=</option><option>IN</option><option>CONTAINS</option></select><input defaultValue="admin" /></div><textarea className="code-editor" value={props.manualRule} onChange={(event) => props.setManualRule(event.target.value)} /><button onClick={props.saveManualRules}>Save Manual Rule Set</button></div><div className="card"><h2>Rule Sets</h2>{props.detail.ruleSets.length ? <div className="records vertical">{props.detail.ruleSets.map((ruleSet) => <article key={ruleSet.id}><div className="record-header"><strong>Version {ruleSet.version}</strong><StatusPill value={ruleSet.status} /></div><span>{ruleSet.confirmedAt ? `Confirmed ${new Date(ruleSet.confirmedAt).toLocaleString()}` : 'Not confirmed'}</span><pre>{pretty(ruleSet.rulesJson)}</pre><button onClick={() => props.confirmRuleSet(ruleSet.id)}>Confirm Rule Set</button></article>)}</div> : <EmptyState title="No rule sets yet" body="Generate rules with a connected AI provider or save a manual rule set from the builder." />}</div></div></section>
}

function AssessmentPage(props: { detail: Detail; selectedEvidenceId: string; setSelectedEvidenceId: (value: string) => void; selectedRuleSetId: string; setSelectedRuleSetId: (value: string) => void; startAssessment: () => void }) {
  const canAssess = Boolean(props.selectedEvidenceId && props.selectedRuleSetId)
  return <section className="page"><PageHeader eyebrow="Page 5" title="Start Security Assessment" subtitle="The Java rule engine evaluates confirmed machine rules against selected structured evidence. AI is not called here." />{canAssess ? <div className="card form-grid"><label>Evidence file<select value={props.selectedEvidenceId} onChange={(event) => props.setSelectedEvidenceId(event.target.value)}>{props.detail.evidenceFiles.map((file) => <option key={file.id} value={file.id}>{file.filename}</option>)}</select></label><label>Rule Set<select value={props.selectedRuleSetId} onChange={(event) => props.setSelectedRuleSetId(event.target.value)}>{props.detail.ruleSets.map((rule) => <option key={rule.id} value={rule.id}>Version {rule.version} · {rule.status}</option>)}</select></label><button className="primary" onClick={props.startAssessment}>Start Assessment</button></div> : <EmptyState title="Assessment is not ready" body="Upload evidence and create or confirm a rule set before starting deterministic assessment." />}<div className="card metrics"><Metric label="Available evidence" value={props.detail.evidenceFiles.length} /><Metric label="Available rule sets" value={props.detail.ruleSets.length} /><Metric label="Previous runs" value={props.detail.assessmentRuns.length} /></div></section>
}

function HistoryPage({ runs }: { runs: Assessment[] }) {
  return <section className="page"><PageHeader eyebrow="Traceability" title="Assessment History and Results" subtitle="Every run stays tied to the evidence file and rule version used at execution time." /><div className="card"><h2>Runs</h2>{runs.length ? <div className="records vertical">{runs.map((run) => <article key={run.id}><div className="record-header"><strong>{new Date(run.createdAt).toLocaleString()}</strong><StatusPill value={run.violationsDetected ? `${run.violationsDetected} VIOLATIONS` : 'PASS'} /></div><span>{run.recordsEvaluated} records · {run.rulesEvaluated} rules · status {run.status}</span><pre>{pretty(run.resultJson)}</pre></article>)}</div> : <EmptyState title="No assessment history yet" body="History appears after you run an assessment from the Assessment page using selected evidence and a rule set." />}</div></section>
}

function BenchmarkPage({ cases, refreshCases, runBenchmark, result }: { cases: BenchmarkCase[]; refreshCases: () => void; runBenchmark: (event: FormEvent<HTMLFormElement>) => void; result: BenchmarkResult | null }) {
  return <section className="page"><PageHeader eyebrow="Page 6" title="AI Reliability Benchmark" subtitle="Run structural AST comparison and deterministic rule-engine execution comparison against human ground truth." /><div className="benchmark-grid"><form className="card form-stack" onSubmit={runBenchmark}><h2>Custom Case</h2><label>Control text<textarea name="controlText" defaultValue="Administrative accounts must use MFA." /></label><label>Test data<textarea name="testDataJson" defaultValue='[{"role":"admin","mfa_enabled":false},{"role":"user","mfa_enabled":true}]' /></label><label>Ground-truth rule<textarea name="groundTruthRuleJson" defaultValue={sampleRule} /></label><label>AI generated rule<textarea name="aiGeneratedRuleJson" defaultValue={sampleRule} /></label><label>Expected result<textarea name="expectedResultJson" placeholder="Optional: paste expected assessment result JSON. Leave blank to execute ground-truth rule." /></label><button className="primary">Run Benchmark Case</button></form><div className="card"><div className="record-header"><h2>Built-in Cases</h2><button onClick={refreshCases}>Reload Cases</button></div>{cases.length ? <div className="records vertical">{cases.map((item) => <article key={item.id}><strong>{item.name}</strong><span>{item.id}</span><p>{item.controlText}</p></article>)}</div> : <EmptyState title="No built-in cases loaded" body="Use Reload Cases to fetch benchmark cases from the backend." />}<h2>Reliability Results</h2><div className="metrics"><Metric label="Total cases" value={result?.totalCases ?? 0} /><Metric label="Accuracy" value={result ? `${Math.round(result.accuracy * 100)}%` : '--'} /><Metric label="AST equivalent" value={result ? (result.structurallyEquivalent ? 'Yes' : 'No') : '--'} /><Metric label="Execution equivalent" value={result ? (result.executionEquivalent ? 'Yes' : 'No') : '--'} /></div>{result ? <div className="records vertical"><article><div className="record-header"><strong>Differences</strong><StatusPill value={result.differences.length ? 'ERROR' : 'PASS'} /></div>{result.differences.length ? result.differences.map((item) => <p key={item.type}>{item.type}: {item.message}</p>) : <p>No reliability difference detected.</p>}</article><article><strong>Generated execution</strong><pre>{pretty(result.generatedExecutionJson)}</pre></article><article><strong>Ground-truth execution</strong><pre>{pretty(result.groundTruthExecutionJson)}</pre></article></div> : <div className="empty-box">Run a benchmark case to see reliability metrics.</div>}</div></div></section>
}

function ApiCoveragePage() {
  const endpoints = ['GET /api/v1/health', 'POST /api/v1/auth/register', 'POST /api/v1/auth/login', 'POST /api/v1/auth/ai-session', 'GET /api/v1/auth/ai-session', 'POST /api/v1/datasets', 'GET /api/v1/datasets', 'GET /api/v1/datasets/{datasetId}', 'DELETE /api/v1/datasets/{datasetId}', 'POST /api/v1/datasets/{datasetId}/controls', 'POST /api/v1/datasets/{datasetId}/evidence', 'DELETE /api/v1/datasets/{datasetId}/evidence/{evidenceId}', 'POST /api/v1/datasets/{datasetId}/rules/generate', 'POST /api/v1/datasets/{datasetId}/rules/manual', 'POST /api/v1/datasets/{datasetId}/rules/{ruleSetId}/confirm', 'POST /api/v1/datasets/{datasetId}/assessments', 'GET /api/v1/benchmarks/cases', 'POST /api/v1/benchmarks/run']
  return <section className="page"><PageHeader eyebrow="API" title="Backend API Coverage" subtitle="Every currently implemented backend endpoint has a visible frontend entry or status here." /><div className="card endpoint-list">{endpoints.map((endpoint) => <div key={endpoint}><code>{endpoint}</code><StatusPill value="WIRED" /></div>)}</div></section>
}

function RequireDataset({ detail, children }: { detail: Detail | null; children: ReactNode }) {
  if (!detail) return <section className="page"><div className="empty-box"><h2>No Dataset Selected</h2><p>Create or select a Dataset on the Datasets page before using this function.</p></div></section>
  return <>{children}</>
}

function EmptyState({ title, body }: { title: string; body: string }) {
  return <div className="empty-box"><h2>{title}</h2><p>{body}</p></div>
}
function PageHeader({ eyebrow, title, subtitle }: { eyebrow: string; title: string; subtitle: string }) {
  return <header className="page-header"><span>{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></header>
}

function UploadCard({ title, description, onFile }: { title: string; description: string; onFile: (file?: File) => void }) {
  return <label className="upload-card"><strong>{title}</strong><span>{description}</span><input type="file" onChange={(event) => onFile(event.target.files?.[0])} /></label>
}

function BackendBadge({ state, onClick }: { state: BackendState; onClick: () => void }) {
  const label = state === 'connected' ? 'Backend connected' : state === 'offline' ? 'Backend offline' : 'Checking backend'
  return <button className={`backend-badge ${state}`} onClick={onClick}><i />{label}</button>
}

function StatusPill({ value }: { value: string }) {
  return <span className={`status-pill ${value.toLowerCase().replaceAll(' ', '-').replaceAll('_', '-')}`}>{value}</span>
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="metric"><strong>{value}</strong><span>{label}</span></div>
}

function DataPreview({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No table preview" body="Select or upload a valid structured evidence file to preview rows here." />
  const columns = Object.keys(rows[0])
  return <div className="table-wrap"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{columns.map((column) => <td key={column}>{String(row[column] ?? '')}</td>)}</tr>)}</tbody></table></div>
}

function parseJsonArray(json?: string) {
  try { return json ? JSON.parse(json) : [] } catch { return [] }
}

function pretty(json: string) {
  try { return JSON.stringify(JSON.parse(json), null, 2) } catch { return json }
}

const sampleRule = JSON.stringify({ rules: [{ id: 'manual-rule-1', fieldsUsed: ['role', 'mfa_enabled'], sourceControl: { controlId: 'IAM-04', text: 'Administrative accounts must use MFA.' }, explanation: 'The IF condition identifies admin accounts, then requires MFA to be enabled.', ast: { type: 'if', if: { type: 'condition', field: 'role', operator: '=', value: 'admin' }, then: { type: 'condition', field: 'mfa_enabled', operator: '=', value: true } } }] }, null, 2)

export default App
