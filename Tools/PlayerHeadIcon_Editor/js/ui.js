import {DEFAULT_COLORS} from './constants.js';
import {state} from './state.js';

export class UIManager {
    constructor() {
        this.paletteContainer = document.getElementById('palette');
        this.colorPicker = document.getElementById('color-picker');
    }

    init() {
        this.initPalette();
        this.updateToolButtons();
        this.updateViewButtons();
    }

    initPalette() {
        DEFAULT_COLORS.forEach(color => {
            const div = document.createElement('div');
            div.className = 'color-swatch';
            div.style.backgroundColor = color;
            div.onclick = () => this.setColor(color);
            this.paletteContainer.appendChild(div);
        });

        // Set initial color
        this.setColor(DEFAULT_COLORS[2]);
    }

    setColor(color) {
        state.currentColor = color;
        this.colorPicker.value = color;

        document.querySelectorAll('.color-swatch').forEach(swatch => {
            const swatchColor = this.rgbToHex(swatch.style.backgroundColor);
            swatch.classList.toggle('active', swatchColor === color);
        });
    }

    rgbToHex(rgb) {
        if (rgb.startsWith('#')) return rgb;
        const [r, g, b] = rgb.match(/\d+/g);
        return "#" + ((1 << 24) + (+r << 16) + (+g << 8) + +b).toString(16).slice(1);
    }

    setTool(tool) {
        state.currentTool = tool;
        this.updateToolButtons();
    }

    updateToolButtons() {
        document.querySelectorAll('.tool-btn').forEach(btn => {
            btn.classList.remove('active');
        });

        const activeBtn = document.getElementById(`tool-${state.currentTool}`);
        if (activeBtn) {
            activeBtn.classList.add('active');
        }

        this.updateSelectionPanel();
    }

    updateSelectionPanel() {
        const panel = document.getElementById('panel-selection');
        if (panel) {
            panel.style.display = state.selection ? 'block' : 'none';
        }
    }

    setView(mode) {
        state.currentView = mode;
        this.updateViewButtons();

        const editorContainer = document.getElementById('editor-container');
        const editorContainer3D = document.getElementById('editor-container-3d');
        const scene = document.querySelector('.scene');
        const previewContainer = document.querySelector('.preview-container');

        if (mode === '3d') {
            editorContainer.style.display = 'none';
            editorContainer3D.style.display = 'flex';

            // Move scene to workspace
            editorContainer3D.appendChild(scene);
            scene.style.transform = `scale(${state.zoom3D}) rotateX(${state.rotX}deg) rotateY(${state.rotY}deg)`;
        } else {
            editorContainer.style.display = 'flex';
            editorContainer3D.style.display = 'none';

            // Move scene back to sidebar
            previewContainer.appendChild(scene);
            scene.style.transform = 'rotateX(-30deg) rotateY(45deg)';
        }
    }

    updateViewButtons() {
        document.getElementById('view-2d').classList.toggle('active', state.currentView === '2d');
        document.getElementById('view-3d').classList.toggle('active', state.currentView === '3d');
    }

    updateBrushSize(value) {
        state.brushSize = parseInt(value);
        document.getElementById('brush-size-val').textContent = state.brushSize;
    }

    updateMirrorMode(checked) {
        state.isMirror = checked;
    }

    updateLayerVisibility() {
        const hatChecked = document.getElementById('toggle-hat').checked;
        const headChecked = document.getElementById('toggle-head').checked;

        document.getElementById('cube-outer').style.display = hatChecked ? 'block' : 'none';
        document.getElementById('cube-inner').style.display = headChecked ? 'block' : 'none';
    }
}
