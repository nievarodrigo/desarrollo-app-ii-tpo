/**
 * Ilustraciones de producto.
 *
 * No usamos fotos de stock: todas las piezas son SVG dibujados acá, sin
 * dependencias ni licencias de terceros, y toman su color de las variables del
 * tema para verse bien en `normal`, `claro` y `oscuro`.
 */

const VIEWBOX = '0 0 320 240';

/** Hash determinístico para que un mismo SKU siempre reciba el mismo fondo. */
function semilla(texto) {
  let h = 0;
  for (let i = 0; i < texto.length; i += 1) {
    h = (h * 31 + texto.charCodeAt(i)) % 100000;
  }
  return h;
}

function fondo(sku) {
  const s = semilla(sku);
  const rot = (s % 40) - 20;
  const cx = 90 + (s % 140);
  const cy = 40 + (s % 60);
  const variante = s % 3;

  const trama =
    variante === 0
      ? `<circle cx="${cx}" cy="${cy}" r="86" fill="var(--art-soft)"/>`
      : variante === 1
        ? `<rect x="${cx - 80}" y="${cy - 40}" width="170" height="170" rx="28" fill="var(--art-soft)" transform="rotate(${rot} ${cx} ${cy})"/>`
        : `<path d="M ${cx - 96} 200 Q ${cx} ${cy - 60} ${cx + 96} 200 Z" fill="var(--art-soft)"/>`;

  return `
    <rect width="320" height="240" fill="var(--art-bg)"/>
    ${trama}
    <rect width="320" height="240" fill="url(#grano)"/>`;
}

/* ------------------------------------------------------------------ */
/* Recetas por objeto. Cada una devuelve markup dentro del mismo lienzo. */
/* ------------------------------------------------------------------ */

const recetas = {
  headphones: () => `
    <path d="M78 148 V116 a82 82 0 0 1 164 0 v32" fill="none" stroke="var(--art-ink)" stroke-width="13" stroke-linecap="round"/>
    <rect x="56" y="132" width="44" height="72" rx="20" fill="var(--art-accent)"/>
    <rect x="220" y="132" width="44" height="72" rx="20" fill="var(--art-accent)"/>
    <rect x="68" y="146" width="20" height="44" rx="10" fill="var(--art-bg)" opacity="0.45"/>
    <circle cx="242" cy="168" r="9" fill="var(--art-bg)" opacity="0.45"/>`,

  laptop: () => `
    <rect x="66" y="62" width="188" height="118" rx="10" fill="var(--art-ink)"/>
    <rect x="78" y="74" width="164" height="94" rx="4" fill="var(--art-accent)"/>
    <path d="M92 148 L124 108 L150 138 L176 96 L212 148 Z" fill="var(--art-bg)" opacity="0.5"/>
    <path d="M44 180 h232 l14 20 H30 Z" fill="var(--art-ink)"/>
    <rect x="132" y="186" width="56" height="6" rx="3" fill="var(--art-bg)" opacity="0.5"/>`,

  keyboard: () => `
    <rect x="40" y="88" width="240" height="104" rx="14" fill="var(--art-ink)"/>
    <rect x="52" y="100" width="216" height="80" rx="8" fill="var(--art-bg)" opacity="0.12"/>
    ${Array.from({ length: 4 }, (_, fila) =>
      Array.from({ length: 9 }, (_, col) => {
        const x = 60 + col * 24;
        const y = 108 + fila * 18;
        const destacada = (fila === 1 && col === 3) || (fila === 2 && col === 6);
        return `<rect x="${x}" y="${y}" width="18" height="13" rx="3" fill="${destacada ? 'var(--art-accent)' : 'var(--art-bg)'}" opacity="${destacada ? 1 : 0.75}"/>`;
      }).join('')
    ).join('')}
    <rect x="112" y="180" width="96" height="13" rx="4" fill="var(--art-bg)" opacity="0.75"/>`,

  lamp: () => `
    <path d="M74 200 h84" stroke="var(--art-ink)" stroke-width="12" stroke-linecap="round"/>
    <path d="M116 200 V132 q0 -66 66 -66 h34" fill="none" stroke="var(--art-ink)" stroke-width="11" stroke-linecap="round"/>
    <path d="M186 58 h74 l-20 46 h-34 Z" fill="var(--art-accent)"/>
    <circle cx="223" cy="112" r="9" fill="var(--art-warm)"/>
    <path d="M199 122 L223 196 L247 122 Z" fill="var(--art-warm)" opacity="0.32"/>`,

  speaker: () => `
    <rect x="96" y="52" width="128" height="150" rx="30" fill="var(--art-ink)"/>
    <circle cx="160" cy="104" r="34" fill="var(--art-accent)"/>
    <circle cx="160" cy="104" r="13" fill="var(--art-bg)" opacity="0.5"/>
    <circle cx="160" cy="162" r="20" fill="var(--art-bg)" opacity="0.18"/>
    <rect x="128" y="196" width="64" height="10" rx="5" fill="var(--art-warm)"/>`,

  chair: () => `
    <path d="M104 54 h96 a16 16 0 0 1 16 16 v66 a16 16 0 0 1 -16 16 h-96 a16 16 0 0 1 -16 -16 V70 a16 16 0 0 1 16 -16 Z" fill="var(--art-accent)"/>
    <path d="M104 78 h96 M104 100 h96 M104 122 h96" stroke="var(--art-bg)" stroke-width="4" opacity="0.4"/>
    <rect x="86" y="152" width="132" height="20" rx="10" fill="var(--art-ink)"/>
    <path d="M152 172 v24 M112 216 l40 -20 l40 20" fill="none" stroke="var(--art-ink)" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>`,

  coffee: () => `
    <path d="M126 24 q10 -13 0 -24 M154 24 q10 -13 0 -24 M182 24 q10 -13 0 -24" fill="none" stroke="var(--art-warm)" stroke-width="6" stroke-linecap="round"/>
    <rect x="104" y="42" width="104" height="22" rx="8" fill="var(--art-ink)"/>
    <rect x="140" y="30" width="32" height="14" rx="6" fill="var(--art-ink)"/>
    <path d="M112 64 h88 l-11 118 a14 14 0 0 1 -14 13 h-38 a14 14 0 0 1 -14 -13 Z" fill="var(--art-accent)"/>
    <path d="M118 122 h76 l-6 60 a14 14 0 0 1 -14 13 h-36 a14 14 0 0 1 -14 -13 Z" fill="var(--art-ink)" opacity="0.35"/>
    <path d="M200 82 q34 6 34 34 t-36 32" fill="none" stroke="var(--art-ink)" stroke-width="11" stroke-linecap="round"/>
    <rect x="98" y="195" width="116" height="13" rx="6" fill="var(--art-ink)"/>`,

  dumbbell: () => `
    <rect x="128" y="110" width="64" height="20" rx="10" fill="var(--art-ink)"/>
    <rect x="96" y="82" width="30" height="76" rx="10" fill="var(--art-accent)"/>
    <rect x="194" y="82" width="30" height="76" rx="10" fill="var(--art-accent)"/>
    <rect x="66" y="96" width="26" height="48" rx="9" fill="var(--art-ink)"/>
    <rect x="228" y="96" width="26" height="48" rx="9" fill="var(--art-ink)"/>
    <path d="M84 186 h152" stroke="var(--art-warm)" stroke-width="10" stroke-linecap="round"/>`,

  monitor: () => `
    <rect x="46" y="48" width="228" height="132" rx="12" fill="var(--art-ink)"/>
    <rect x="58" y="60" width="204" height="108" rx="5" fill="var(--art-accent)"/>
    <rect x="76" y="80" width="72" height="10" rx="5" fill="var(--art-bg)" opacity="0.6"/>
    <rect x="76" y="100" width="128" height="8" rx="4" fill="var(--art-bg)" opacity="0.35"/>
    <rect x="76" y="118" width="104" height="8" rx="4" fill="var(--art-bg)" opacity="0.35"/>
    <rect x="146" y="180" width="28" height="24" fill="var(--art-ink)"/>
    <rect x="106" y="200" width="108" height="12" rx="6" fill="var(--art-ink)"/>`,

  bottle: () => `
    <rect x="140" y="30" width="40" height="26" rx="6" fill="var(--art-ink)"/>
    <path d="M132 56 h56 q22 0 22 26 v100 q0 26 -26 26 h-48 q-26 0 -26 -26 V82 q0 -26 22 -26 Z" fill="var(--art-accent)"/>
    <path d="M138 96 h44" stroke="var(--art-bg)" stroke-width="7" opacity="0.5" stroke-linecap="round"/>
    <path d="M138 116 h30" stroke="var(--art-bg)" stroke-width="7" opacity="0.3" stroke-linecap="round"/>
    <rect x="110" y="140" width="100" height="26" fill="var(--art-warm)" opacity="0.55"/>`,

  desk: () => `
    <rect x="42" y="96" width="236" height="18" rx="6" fill="var(--art-warm)"/>
    <rect x="66" y="114" width="20" height="86" rx="6" fill="var(--art-ink)"/>
    <rect x="234" y="114" width="20" height="86" rx="6" fill="var(--art-ink)"/>
    <path d="M56 200 h40 M224 200 h40" stroke="var(--art-ink)" stroke-width="11" stroke-linecap="round"/>
    <rect x="122" y="52" width="76" height="44" rx="6" fill="var(--art-accent)"/>
    <path d="M282 122 v-30 l-14 12 M282 92 l14 12" fill="none" stroke="var(--art-accent)" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>`,

  watch: () => `
    <rect x="132" y="26" width="56" height="52" rx="18" fill="var(--art-ink)"/>
    <rect x="132" y="162" width="56" height="52" rx="18" fill="var(--art-ink)"/>
    <rect x="110" y="62" width="100" height="118" rx="28" fill="var(--art-ink)"/>
    <rect x="122" y="74" width="76" height="94" rx="20" fill="var(--art-accent)"/>
    <path d="M136 132 l14 -20 l14 26 l14 -38 l14 32" fill="none" stroke="var(--art-bg)" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
    <rect x="210" y="102" width="9" height="26" rx="4" fill="var(--art-warm)"/>`,

  blanket: () => `
    <path d="M56 78 q52 -26 104 0 t104 0 v96 q-52 26 -104 0 t-104 0 Z" fill="var(--art-accent)"/>
    <path d="M56 110 q52 -26 104 0 t104 0" fill="none" stroke="var(--art-bg)" stroke-width="6" opacity="0.4"/>
    <path d="M56 142 q52 -26 104 0 t104 0" fill="none" stroke="var(--art-bg)" stroke-width="6" opacity="0.4"/>
    <path d="M56 174 v30 q52 26 104 0 t104 0 v-30" fill="var(--art-warm)" opacity="0.6"/>`,

  mic: () => `
    <rect x="132" y="34" width="56" height="106" rx="28" fill="var(--art-accent)"/>
    <path d="M140 52 h40 M140 68 h40 M140 84 h40 M140 100 h40" stroke="var(--art-bg)" stroke-width="5" opacity="0.45"/>
    <path d="M104 116 a56 56 0 0 0 112 0" fill="none" stroke="var(--art-ink)" stroke-width="11" stroke-linecap="round"/>
    <path d="M160 172 v28" stroke="var(--art-ink)" stroke-width="11" stroke-linecap="round"/>
    <path d="M116 208 l44 -20 l44 20" fill="none" stroke="var(--art-ink)" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>`,

  backpack: () => `
    <path d="M120 62 a40 40 0 0 1 80 0" fill="none" stroke="var(--art-ink)" stroke-width="12"/>
    <path d="M96 82 h128 q22 0 22 24 v82 q0 22 -22 22 h-128 q-22 0 -22 -22 v-82 q0 -24 22 -24 Z" fill="var(--art-accent)"/>
    <rect x="106" y="128" width="108" height="46" rx="12" fill="var(--art-bg)" opacity="0.28"/>
    <rect x="140" y="96" width="40" height="18" rx="9" fill="var(--art-warm)"/>
    <path d="M96 174 h128" stroke="var(--art-ink)" stroke-width="6" opacity="0.35"/>`,

  organizer: () => `
    <rect x="52" y="112" width="216" height="80" rx="10" fill="var(--art-warm)"/>
    <rect x="66" y="126" width="58" height="52" rx="6" fill="var(--art-bg)" opacity="0.35"/>
    <rect x="134" y="126" width="58" height="52" rx="6" fill="var(--art-bg)" opacity="0.35"/>
    <rect x="202" y="126" width="52" height="52" rx="6" fill="var(--art-bg)" opacity="0.35"/>
    <rect x="80" y="62" width="10" height="56" rx="5" fill="var(--art-accent)"/>
    <rect x="98" y="76" width="10" height="42" rx="5" fill="var(--art-ink)"/>
    <path d="M212 74 q22 12 0 24 t0 24" fill="none" stroke="var(--art-ink)" stroke-width="7" stroke-linecap="round"/>`,

  mouse: () => `
    <path d="M160 46 c46 0 66 34 66 78 v26 c0 40 -28 64 -66 64 s-66 -24 -66 -64 v-26 c0 -44 20 -78 66 -78 Z" fill="var(--art-ink)"/>
    <path d="M160 46 v62 M94 112 h132" stroke="var(--art-bg)" stroke-width="4" opacity="0.35"/>
    <rect x="152" y="66" width="16" height="30" rx="8" fill="var(--art-accent)"/>
    <path d="M118 178 q42 22 84 0" fill="none" stroke="var(--art-bg)" stroke-width="5" opacity="0.25" stroke-linecap="round"/>`,

  tablet: () => `
    <rect x="86" y="34" width="148" height="182" rx="18" fill="var(--art-ink)"/>
    <rect x="98" y="50" width="124" height="146" rx="6" fill="var(--art-accent)"/>
    <rect x="112" y="68" width="96" height="10" rx="5" fill="var(--art-bg)" opacity="0.6"/>
    <rect x="112" y="88" width="70" height="8" rx="4" fill="var(--art-bg)" opacity="0.45"/>
    <rect x="112" y="112" width="96" height="62" rx="8" fill="var(--art-warm)" opacity="0.8"/>
    <circle cx="160" cy="206" r="4" fill="var(--art-bg)" opacity="0.5"/>`,

  phone: () => `
    <rect x="116" y="28" width="88" height="186" rx="18" fill="var(--art-ink)"/>
    <rect x="124" y="42" width="72" height="156" rx="10" fill="var(--art-accent)"/>
    <rect x="146" y="34" width="28" height="5" rx="2.5" fill="var(--art-bg)" opacity="0.5"/>
    <circle cx="160" cy="104" r="22" fill="var(--art-bg)" opacity="0.35"/>
    <rect x="136" y="144" width="48" height="8" rx="4" fill="var(--art-bg)" opacity="0.55"/>
    <rect x="144" y="160" width="32" height="8" rx="4" fill="var(--art-bg)" opacity="0.4"/>`,

  tv: () => `
    <rect x="38" y="44" width="244" height="142" rx="10" fill="var(--art-ink)"/>
    <rect x="48" y="54" width="224" height="122" rx="4" fill="var(--art-accent)"/>
    <path d="M48 176 L118 106 L162 150 L206 96 L272 162 V176 Z" fill="var(--art-bg)" opacity="0.4"/>
    <circle cx="230" cy="84" r="14" fill="var(--art-warm)"/>
    <path d="M112 212 l20 -26 h56 l20 26" fill="none" stroke="var(--art-ink)" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>`,

  camera: () => `
    <path d="M118 74 l12 -20 h60 l12 20" fill="var(--art-ink)"/>
    <rect x="58" y="72" width="204" height="128" rx="18" fill="var(--art-ink)"/>
    <circle cx="160" cy="136" r="48" fill="var(--art-bg)" opacity="0.2"/>
    <circle cx="160" cy="136" r="36" fill="var(--art-accent)"/>
    <circle cx="160" cy="136" r="16" fill="var(--art-ink)"/>
    <circle cx="150" cy="126" r="6" fill="var(--art-bg)" opacity="0.7"/>
    <rect x="216" y="88" width="28" height="14" rx="5" fill="var(--art-warm)"/>`,

  plant: () => `
    <path d="M160 140 C150 104 116 92 92 98 C100 126 126 142 160 140 Z" fill="var(--art-accent)"/>
    <path d="M160 132 C170 92 204 74 232 80 C224 112 196 132 160 132 Z" fill="var(--art-accent)" opacity="0.8"/>
    <path d="M160 140 C156 102 160 70 176 44 C188 74 182 110 160 140 Z" fill="var(--art-warm)"/>
    <path d="M160 150 V110" stroke="var(--art-ink)" stroke-width="5" stroke-linecap="round"/>
    <path d="M108 146 h104 l-14 64 h-76 Z" fill="var(--art-ink)"/>
    <rect x="102" y="140" width="116" height="16" rx="6" fill="var(--art-ink)"/>`,

  mug: () => `
    <path d="M200 98 h18 a28 28 0 0 1 0 56 h-18" fill="none" stroke="var(--art-ink)" stroke-width="12"/>
    <path d="M90 82 h116 v96 q0 30 -30 30 h-56 q-30 0 -30 -30 Z" fill="var(--art-accent)"/>
    <rect x="90" y="82" width="116" height="16" rx="4" fill="var(--art-ink)" opacity="0.25"/>
    <path d="M124 70 q-10 -16 0 -30 M148 70 q-10 -16 0 -30 M172 70 q-10 -16 0 -30" fill="none" stroke="var(--art-ink)" stroke-width="5" stroke-linecap="round" opacity="0.4"/>
    <path d="M108 130 h80" stroke="var(--art-bg)" stroke-width="6" opacity="0.35" stroke-linecap="round"/>`,

  pan: () => `
    <ellipse cx="136" cy="140" rx="92" ry="34" fill="var(--art-ink)"/>
    <ellipse cx="136" cy="132" rx="80" ry="26" fill="var(--art-accent)"/>
    <ellipse cx="124" cy="128" rx="34" ry="10" fill="var(--art-bg)" opacity="0.3"/>
    <path d="M222 132 L292 112" stroke="var(--art-ink)" stroke-width="16" stroke-linecap="round"/>
    <circle cx="282" cy="115" r="3.5" fill="var(--art-bg)" opacity="0.6"/>`,

  mat: () => `
    <path d="M48 176 h170 v26 H64 q-16 0 -16 -13 Z" fill="var(--art-accent)"/>
    <path d="M60 189 h150" stroke="var(--art-bg)" stroke-width="4" opacity="0.35"/>
    <circle cx="226" cy="162" r="40" fill="var(--art-accent)"/>
    <circle cx="226" cy="162" r="40" fill="none" stroke="var(--art-ink)" stroke-width="4" opacity="0.2"/>
    <circle cx="226" cy="162" r="26" fill="var(--art-ink)" opacity="0.18"/>
    <circle cx="226" cy="162" r="12" fill="var(--art-ink)" opacity="0.3"/>
    <path d="M226 122 q40 0 40 40" fill="none" stroke="var(--art-bg)" stroke-width="5" opacity="0.35" stroke-linecap="round"/>`,

  ball: () => `
    <circle cx="160" cy="122" r="80" fill="var(--art-bg)"/>
    <circle cx="160" cy="122" r="80" fill="none" stroke="var(--art-ink)" stroke-width="8"/>
    <path d="M160 92 l28 20 -10 33 h-36 l-10 -33 Z" fill="var(--art-ink)"/>
    <path d="M160 92 V52 M188 112 l38 -12 M178 145 l24 32 M142 145 l-24 32 M132 112 l-38 -12" stroke="var(--art-ink)" stroke-width="6"/>
    <path d="M140 54 l20 -8 20 8 M226 100 l4 22 -10 18 M94 100 l-4 22 10 18" fill="none" stroke="var(--art-accent)" stroke-width="8" stroke-linecap="round"/>
    <ellipse cx="160" cy="214" rx="56" ry="7" fill="var(--art-ink)" opacity="0.18"/>`,

  shoe: () => `
    <path d="M52 150 c0 -26 16 -52 42 -58 l32 22 c22 14 50 18 84 22 c32 4 56 18 56 40 v10 H52 Z" fill="var(--art-accent)"/>
    <path d="M46 184 h234 q8 0 8 -8 v-6 H46 Z" fill="var(--art-ink)"/>
    <path d="M110 112 l16 -10 M124 122 l16 -10 M138 132 l16 -10" stroke="var(--art-bg)" stroke-width="6" stroke-linecap="round" opacity="0.7"/>
    <path d="M60 160 q90 10 210 6" fill="none" stroke="var(--art-bg)" stroke-width="5" opacity="0.4"/>`,

  printer: () => `
    <rect x="102" y="40" width="116" height="56" rx="4" fill="var(--art-bg)" stroke="var(--art-ink)" stroke-width="6"/>
    <rect x="56" y="88" width="208" height="88" rx="16" fill="var(--art-ink)"/>
    <rect x="96" y="150" width="128" height="64" rx="3" fill="var(--art-bg)"/>
    <path d="M114 170 h92 M114 186 h72" stroke="var(--art-ink)" stroke-width="5" stroke-linecap="round" opacity="0.35"/>
    <circle cx="232" cy="114" r="7" fill="var(--art-accent)"/>
    <rect x="84" y="108" width="62" height="8" rx="4" fill="var(--art-bg)" opacity="0.3"/>`
};

/**
 * Devuelve el `<svg>` completo de un producto.
 * El patrón `#grano` se define una única vez en `index.html` para no repetir
 * ids dentro del documento.
 * @param {{sku:string, arte:string, nombre:string}} producto
 */
export function arteProducto(producto) {
  const receta = recetas[producto.arte] ?? recetas.headphones;
  return `<svg class="arte" viewBox="${VIEWBOX}" role="img" aria-label="Ilustración de ${producto.nombre}" preserveAspectRatio="xMidYMid slice">
    ${fondo(producto.sku)}
    <g class="arte__objeto">${receta()}</g>
  </svg>`;
}
