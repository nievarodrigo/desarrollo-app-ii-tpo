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
    <path d="M212 74 q22 12 0 24 t0 24" fill="none" stroke="var(--art-ink)" stroke-width="7" stroke-linecap="round"/>`
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
