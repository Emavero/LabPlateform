import { useState } from 'react';
import {
  REPORT_WINDOWS,
  hoursAndMinutes,
  reportFileName,
  toMarkdown,
  type ReportLabels,
} from '@/domain/models/Report';
import { Alert, Button, Panel, Spinner } from '../design-system';
import { StatTile } from '../features/analytics/StatTile';
import { RankingBars } from '../features/analytics/RankingBars';
import { saveTextFile } from '../features/vpn/saveFile';
import { useI18n } from '../i18n/I18nContext';
import { useReport } from '../hooks/useReport';

/**
 * Centre de rapports.
 * <p>
 * Chaque chiffre est montré avec celui de la période précédente : un total seul
 * ne dit pas s'il monte ou s'il descend. Les échecs y figurent au même titre
 * que les réussites — un mois à trente flags refusés et deux validés raconte
 * quelque chose qu'aucun des deux chiffres ne dit seul.
 */
export function ReportCenterPage() {
  const { t, formatDate } = useI18n();
  const { report, days, loading, error, setDays, reload } = useReport();
  const [saved, setSaved] = useState(false);

  const labels = (): ReportLabels => ({
    title: t('report.title'),
    period: t('report.period'),
    summary: t('report.summary'),
    flags: t('report.flags'),
    boxes: t('report.boxes'),
    points: t('report.points'),
    sections: t('report.sections'),
    minutes: t('report.minutes'),
    activeDays: t('report.activeDays'),
    quizzes: t('report.quizzes'),
    families: t('report.families'),
    machines: t('report.machines'),
    courses: t('report.courses'),
    none: t('report.none'),
    generated: t('report.generated'),
  });

  const download = () => {
    if (!report) return;
    saveTextFile(reportFileName(report), toMarkdown(report, labels(), formatDate), 'text/markdown;charset=utf-8');
    setSaved(true);
  };

  const studied = report ? hoursAndMinutes(report.totals.minutesStudied) : null;
  // Les minutes sont complétées à deux chiffres : « 3 h 05 » et non « 3 h 5 ».
  const studiedLabel =
    report && studied && report.totals.minutesStudied > 0
      ? t('report.duration', { hours: studied.hours, minutes: String(studied.minutes).padStart(2, '0') })
      : undefined;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('nav.reports')}</p>
          <h1 className="page__title">{t('report.title')}</h1>
          <p className="page__lead">{t('report.lead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      <div className="tabs" role="tablist" aria-label={t('report.period')}>
        {REPORT_WINDOWS.map((window) => (
          <button
            key={window}
            type="button"
            role="tab"
            aria-selected={days === window}
            className={['tabs__tab', days === window && 'tabs__tab--active'].filter(Boolean).join(' ')}
            onClick={() => {
              setSaved(false);
              setDays(window);
            }}
          >
            {t(`report.window.${window}` as 'report.window.7')}
          </button>
        ))}
      </div>

      {error && (
        <Alert tone="error" title={t('report.loadError')}>
          {error.message}
        </Alert>
      )}

      {loading && !report ? (
        <div className="empty">
          <Spinner size={22} label={t('report.loading')} />
        </div>
      ) : (
        report &&
        studied && (
          <>
            <header className="report__head">
              <p className="report__range">
                {t('report.range', { from: formatDate(report.from), to: formatDate(report.to) })}
              </p>
              <Button variant="ghost" size="sm" icon="download" onClick={download}>
                {t('report.export')}
              </Button>
            </header>
            {saved && <Alert tone="success">{t('report.exported')}</Alert>}

            {report.totals.events === 0 ? (
              <Alert tone="info">{t('report.idle')}</Alert>
            ) : (
              <>
                <div className="kpi-row">
                  <StatTile label={t('report.flags')} value={report.totals.flags} previous={report.previous.flags} />
                  <StatTile
                    label={t('report.boxes')}
                    value={report.totals.boxesPwned}
                    previous={report.previous.boxesPwned}
                  />
                  <StatTile label={t('report.points')} value={report.totals.points} previous={report.previous.points} />
                  <StatTile
                    label={t('report.sections')}
                    value={report.totals.sections}
                    previous={report.previous.sections}
                  />
                  <StatTile
                    label={t('report.activeDays')}
                    value={report.totals.activeDays}
                    previous={report.previous.activeDays}
                  />
                  {/* Sans quiz rendu, la tuile affiche un tiret : 0 % se lirait « tout manqué ». */}
                  <StatTile
                    label={t('report.quizRate')}
                    value={report.totals.quizSuccessPercent}
                    unit="%"
                    hint={t('report.quizzes')}
                  />
                </div>

                <div className="page__grid">
                  <Panel title={t('report.families')} description={studiedLabel}>
                    {report.families.length === 0 ? (
                      <p className="empty">{t('report.none')}</p>
                    ) : (
                      <RankingBars
                        insight={{
                          code: 'families',
                          title: t('report.families'),
                          unit: t('report.eventsUnit'),
                          entries: report.families.map((family) => ({ subject: family.label, count: family.count })),
                        }}
                      />
                    )}
                  </Panel>

                  <Panel title={t('report.kinds')}>
                    {report.kinds.length === 0 ? (
                      <p className="empty">{t('report.none')}</p>
                    ) : (
                      <ul className="tally">
                        {report.kinds.map((kind) => (
                          <li key={kind.code} className="tally__row">
                            <span className="tally__label">{kind.label}</span>
                            <span className="tally__count">{kind.count}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </Panel>

                  <Panel title={t('report.machines')}>
                    {report.machines.length === 0 ? (
                      <p className="empty">{t('report.none')}</p>
                    ) : (
                      <ul className="tally">
                        {report.machines.map((machine) => (
                          <li key={machine.slug} className="tally__row">
                            <span className="tally__label">
                              {machine.name}
                              <span className="tally__note">
                                {machine.difficulty} ·{' '}
                                {t(machine.pwned ? 'report.pwned' : 'report.partial')} ·{' '}
                                {t('report.flagsCount', { count: machine.flags })}
                              </span>
                            </span>
                            <span className="tally__count">{machine.points}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </Panel>

                  <Panel title={t('report.courses')}>
                    {report.courses.length === 0 ? (
                      <p className="empty">{t('report.none')}</p>
                    ) : (
                      <ul className="tally">
                        {report.courses.map((course) => (
                          <li key={course.slug} className="tally__row">
                            <span className="tally__label">
                              {course.title}
                              <span className="tally__note">
                                {course.track} · {t('report.sectionsDone', { count: course.sections })}
                              </span>
                            </span>
                            <span className="tally__count">{t('courses.minutes', { minutes: course.minutes })}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </Panel>
                </div>
              </>
            )}
          </>
        )
      )}
    </div>
  );
}
