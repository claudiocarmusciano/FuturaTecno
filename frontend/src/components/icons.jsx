// Íconos finos (line style), heredan el color del texto (currentColor).

export const IconEdit = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
  </svg>
)

export const IconSearch = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </svg>
)

export const IconTrash = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 6h18" />
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
  </svg>
)

// Íconos en línea con el texto: miden 1em y heredan el color, así reemplazan a un emoji sin CSS
// extra. Los emojis se ven distintos en cada sistema (y en color); estos son iguales en todos.
const Icono = ({ size = '1em', className = '', title, children, ...rest }) => (
  <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth="2"
    strokeLinecap="round" strokeLinejoin="round" className={`icono ${className}`.trim()}
    aria-hidden={title ? undefined : 'true'} role={title ? 'img' : undefined} focusable="false"
    style={{ verticalAlign: '-0.125em', flexShrink: 0 }} {...rest}>
    {title && <title>{title}</title>}
    {children}
  </svg>
)

export const IconCart = (p) => (
  <Icono {...p}><circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" /><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" /></Icono>
)
export const IconInfo = (p) => (
  <Icono {...p}><circle cx="12" cy="12" r="10" /><path d="M12 16v-4" /><path d="M12 8h.01" /></Icono>
)
export const IconMail = (p) => (
  <Icono {...p}><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-10 6L2 7" /></Icono>
)
export const IconCheck = (p) => (
  <Icono {...p}><path d="M20 6 9 17l-5-5" /></Icono>
)
export const IconCheckCircle = (p) => (
  <Icono {...p}><circle cx="12" cy="12" r="10" /><path d="m9 12 2 2 4-4" /></Icono>
)
export const IconX = (p) => (
  <Icono {...p}><path d="M18 6 6 18" /><path d="m6 6 12 12" /></Icono>
)
export const IconMenu = (p) => (
  <Icono {...p}><path d="M4 6h16" /><path d="M4 12h16" /><path d="M4 18h16" /></Icono>
)
export const IconAlert = (p) => (
  <Icono {...p}><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><path d="M12 9v4" /><path d="M12 17h.01" /></Icono>
)
export const IconBulb = (p) => (
  <Icono {...p}><path d="M9 18h6" /><path d="M10 22h4" /><path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5A4.61 4.61 0 0 1 8.91 14" /></Icono>
)
export const IconLink = (p) => (
  <Icono {...p}><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" /></Icono>
)
export const IconChat = (p) => (
  <Icono {...p}><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" /></Icono>
)
export const IconTruck = (p) => (
  <Icono {...p}><path d="M1 3h15v13H1z" /><path d="M16 8h4l3 3v5h-7V8z" /><circle cx="5.5" cy="18.5" r="2.5" /><circle cx="18.5" cy="18.5" r="2.5" /></Icono>
)
export const IconArrowUpRight = (p) => (
  <Icono {...p}><path d="M7 17 17 7" /><path d="M7 7h10v10" /></Icono>
)
export const IconGrid = (p) => (
  <Icono {...p}><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /></Icono>
)
export const IconBanknote = (p) => (
  <Icono {...p}><rect x="2" y="6" width="20" height="12" rx="2" /><circle cx="12" cy="12" r="2" /><path d="M6 12h.01M18 12h.01" /></Icono>
)
export const IconStar = (p) => (
  <Icono {...p}><path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></Icono>
)

// Componentes de Armá tu PC.
export const IconCpu = (p) => (
  <Icono {...p}><rect x="4" y="4" width="16" height="16" rx="2" /><rect x="9" y="9" width="6" height="6" /><path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 14h3M1 9h3M1 14h3" /></Icono>
)
export const IconMotherboard = (p) => (
  <Icono {...p}><rect x="3" y="3" width="18" height="18" rx="2" /><rect x="7" y="7" width="5" height="5" /><path d="M15 7h2M15 10h2M7 15.5h10M7 18h6" /></Icono>
)
export const IconMemory = (p) => (
  <Icono {...p}><rect x="2" y="7" width="20" height="10" rx="1" /><path d="M6 10v4M10 10v4M14 10v4M18 10v4M5 17v3M9 17v3M15 17v3M19 17v3" /></Icono>
)
export const IconGpu = (p) => (
  <Icono {...p}><rect x="2" y="6" width="20" height="11" rx="2" /><circle cx="9" cy="11.5" r="3" /><path d="M15 9.5h4M15 13.5h4M5 17v3M9 17v2" /></Icono>
)
export const IconHardDrive = (p) => (
  <Icono {...p}><path d="M22 12H2" /><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" /><path d="M6 16h.01M10 16h.01" /></Icono>
)
export const IconDisc = (p) => (
  <Icono {...p}><circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="3" /></Icono>
)
export const IconPlug = (p) => (
  <Icono {...p}><path d="M12 22v-5" /><path d="M9 8V2" /><path d="M15 8V2" /><path d="M18 8v5a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V8Z" /></Icono>
)
export const IconTower = (p) => (
  <Icono {...p}><rect x="6" y="2" width="12" height="20" rx="2" /><path d="M10 6h4M10 9.5h4" /><circle cx="12" cy="16" r="2" /></Icono>
)
export const IconFan = (p) => (
  <Icono {...p}><path d="M10.827 16.379a6.082 6.082 0 0 1-8.618-7.002l5.412 1.45a6.082 6.082 0 0 1 7.002-8.618l-1.45 5.412a6.082 6.082 0 0 1 8.618 7.002l-5.412-1.45a6.082 6.082 0 0 1-7.002 8.618l1.45-5.412Z" /><path d="M12 12v.01" /></Icono>
)
// La IconSearch de arriba es del admin (sin tamaño propio); esta va en línea con el texto.
export const IconSearchLine = (p) => (
  <Icono {...p}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></Icono>
)
