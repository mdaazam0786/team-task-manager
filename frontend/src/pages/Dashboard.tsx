import { useEffect, useState } from 'react'
import { api, type DashboardOverviewResponse, type DashboardRange } from '../api'
import { Card, ErrorBanner } from '../ui'

export function DashboardPage() {
  const [data, setData] = useState<DashboardOverviewResponse | null>(null)
  const [range, setRange] = useState<DashboardRange>('monthly')
  const [chartPoints, setChartPoints] = useState<Array<{ label: string; value: number }>>([])
  const [error, setError] = useState<string | null>(null)

  function normalizeChartPoints(payload: unknown): Array<{ label: string; value: number }> {
    if (!payload || typeof payload !== 'object') return []
    const obj = payload as Record<string, unknown>

    // Shape A: { points: [{ label, value }] }
    const pointsA = obj.points
    if (Array.isArray(pointsA)) {
      const out = pointsA
        .map((p) => {
          if (!p || typeof p !== 'object') return null
          const r = p as Record<string, unknown>
          const label = typeof r.label === 'string' ? r.label : typeof r.date === 'string' ? r.date : null
          const value = typeof r.value === 'number' ? r.value : typeof r.count === 'number' ? r.count : null
          if (!label || value === null) return null
          return { label, value }
        })
        .filter(Boolean) as Array<{ label: string; value: number }>
      if (out.length) return out
    }

    // Shape B: { data: [{ date, count }] } or similar
    const pointsB = obj.data
    if (Array.isArray(pointsB)) {
      return pointsB
        .map((p) => {
          if (!p || typeof p !== 'object') return null
          const r = p as Record<string, unknown>
          const label = typeof r.label === 'string' ? r.label : typeof r.date === 'string' ? r.date : null
          const value =
            typeof r.value === 'number'
              ? r.value
              : typeof r.count === 'number'
                ? r.count
                : typeof r.done === 'number'
                  ? r.done
                  : null
          if (!label || value === null) return null
          return { label, value }
        })
        .filter(Boolean) as Array<{ label: string; value: number }>
    }

    return []
  }

  useEffect(() => {
    let mounted = true
    Promise.all([api.dashboardOverview(), api.dashboardChart(range)])
      .then(([overview, c]) => {
        if (!mounted) return
        setData(overview)
        setChartPoints(normalizeChartPoints(c))
      })
      .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Failed to load dashboard'))
    const onRefresh = () => {
      api
        .dashboardOverview()
        .then((overview) => mounted && setData(overview))
        .catch(() => {
          // ignore; existing error banner already covers initial load
        })
    }
    window.addEventListener('dashboard:refresh', onRefresh)
    return () => {
      mounted = false
      window.removeEventListener('dashboard:refresh', onRefresh)
    }
  }, [range])

  const values = chartPoints.map((p) => p.value)
  const labels = chartPoints.map((p) => p.label)
  const max = values.length ? Math.max(...values) : 1
  const min = values.length ? Math.min(...values) : 0
  const span = max - min || 1
  const w = 640
  const h = 220
  const padX = 40
  const padY = 24
  const toXY = (v: number, i: number) => {
    const x = values.length <= 1 ? padX : (i / (values.length - 1)) * (w - padX * 2) + padX
    const y = (1 - (v - min) / span) * (h - padY * 2) + padY
    return [x, y] as const
  }
  const lineD =
    values.length === 0
      ? ''
      : values
          .map((v, i) => toXY(v, i))
          .map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`)
          .join(' ')
  const areaD =
    values.length === 0
      ? ''
      : `${lineD} L${(w - padX).toFixed(1)},${(h - padY).toFixed(1)} L${padX.toFixed(1)},${(h - padY).toFixed(1)} Z`

  return (
    <div className="dash">
      <div className="dashTop">
        <div>
          <h1 className="dashTitle">Overview</h1>
          <div className="muted small">Track progress across your team projects.</div>
        </div>
      </div>

      <ErrorBanner message={error} />

      <div className="dashGrid">
        <section className="dashMain">
          <div className="kpis">
            <Card>
              <div className="kpi">
                <div>
                  <div className="kpiLabel">Tasks completed</div>
                  <div className="kpiValue">{data?.tasksDone ?? 0}</div>
                </div>
              </div>
            </Card>
            <Card>
              <div className="kpi">
                <div>
                  <div className="kpiLabel">In progress</div>
                  <div className="kpiValue">{data?.tasksInProgress ?? 0}</div>
                </div>
              </div>
            </Card>
            <Card>
              <div className="kpi">
                <div>
                  <div className="kpiLabel">New tasks</div>
                  <div className="kpiValue">{data?.newTasks ?? 0}</div>
                </div>
              </div>
            </Card>
            <Card>
              <div className="kpi">
                <div>
                  <div className="kpiLabel">Projects done</div>
                  <div className="kpiValue">{data?.projectsDone ?? 0}</div>
                </div>
              </div>
            </Card>
            {/* Overdue card — shown only when the backend returns the field */}
            {data?.overdue !== undefined ? (
              <Card>
                <div className="kpi">
                  <div>
                    <div className="kpiLabel kpiLabelDanger">Overdue</div>
                    <div className="kpiValue kpiValueDanger">{data.overdue}</div>
                  </div>
                </div>
              </Card>
            ) : null}
          </div>

          <div className="dashSplit">
            <Card>
              <div className="panelHead">
                <div>
                  <h2 className="panelTitle">Tasks Done</h2>
                  <div className="muted small">{range[0].toUpperCase() + range.slice(1)}</div>
                </div>
                <div className="seg">
                  <button className={`segBtn ${range === 'daily' ? 'active' : ''}`} onClick={() => setRange('daily')}>
                    Daily
                  </button>
                  <button className={`segBtn ${range === 'weekly' ? 'active' : ''}`} onClick={() => setRange('weekly')}>
                    Weekly
                  </button>
                  <button className={`segBtn ${range === 'monthly' ? 'active' : ''}`} onClick={() => setRange('monthly')}>
                    Monthly
                  </button>
                </div>
              </div>

              <div className="bigChart">
                <svg viewBox="0 0 640 220" className="bigChartSvg" aria-hidden="true">
                  <path className="bigChartGrid" d="M40 190H600" />
                  <path className="bigChartGrid" d="M40 130H600" />
                  <path className="bigChartGrid" d="M40 70H600" />
                  {areaD ? <path className="bigChartArea" d={areaD} /> : null}
                  {lineD ? <path className="bigChartLine a" d={lineD} /> : null}
                </svg>
                <div className="bigChartAxis muted small">
                  {labels.length
                    ? labels.map((l) => (
                        <span key={l} title={l}>
                          {l}
                        </span>
                      ))
                    : null}
                </div>
              </div>
            </Card>
          </div>
        </section>
      </div>
    </div>
  )
}
