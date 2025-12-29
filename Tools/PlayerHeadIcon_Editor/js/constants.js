// Canvas and Skin Constants
export const CANVAS_SCALE = 10;
export const SKIN_WIDTH = 64;
export const SKIN_HEIGHT = 64;

// Visual Layout Constants
export const INNER_OFFSET_X = 0;
export const OUTER_OFFSET_X = 40;

// Mapping Definitions: [VisualX, VisualY, SourceX, SourceY, Width, Height]
export const MAPPINGS = [
    // Inner Head
    { vx: 8,  vy: 0,  sx: 8,  sy: 0,  w: 8, h: 8, name: 'Top' },
    { vx: 8,  vy: 16, sx: 16, sy: 0,  w: 8, h: 8, name: 'Bottom' },
    { vx: 0,  vy: 8,  sx: 0,  sy: 8,  w: 8, h: 8, name: 'Right' },
    { vx: 8,  vy: 8,  sx: 8,  sy: 8,  w: 8, h: 8, name: 'Front' },
    { vx: 16, vy: 8,  sx: 16, sy: 8,  w: 8, h: 8, name: 'Left' },
    { vx: 24, vy: 8,  sx: 24, sy: 8,  w: 8, h: 8, name: 'Back' },

    // Outer Hat (Offset X by 40)
    { vx: 48, vy: 0,  sx: 40, sy: 0,  w: 8, h: 8, name: 'Hat Top' },
    { vx: 48, vy: 16, sx: 48, sy: 0,  w: 8, h: 8, name: 'Hat Bottom' },
    { vx: 40, vy: 8,  sx: 32, sy: 8,  w: 8, h: 8, name: 'Hat Right' },
    { vx: 48, vy: 8,  sx: 40, sy: 8,  w: 8, h: 8, name: 'Hat Front' },
    { vx: 56, vy: 8,  sx: 48, sy: 8,  w: 8, h: 8, name: 'Hat Left' },
    { vx: 64, vy: 8,  sx: 56, sy: 8,  w: 8, h: 8, name: 'Hat Back' }
];

// History Constants
export const MAX_HISTORY = 50;

// Default Colors Palette
export const DEFAULT_COLORS = [
    '#1a1a1a', '#ffffff', '#b4202a', '#1e7432', '#193d82',
    '#f0c426', '#f27f18', '#9c4e26', '#4a2c18', '#d98e68'
];
