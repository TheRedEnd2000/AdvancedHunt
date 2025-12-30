import {MAX_HISTORY} from './constants.js';

export class HistoryManager {
    constructor(canvasManager, onStateRestored = null) {
        this.canvasManager = canvasManager;
        this.history = [];
        this.historyStep = -1;
        this.onStateRestored = onStateRestored;
        this._restoreSeq = 0;
    }

    setOnStateRestored(callback) {
        this.onStateRestored = callback;
    }

    saveState() {
        const dataCanvas = this.canvasManager.getDataCanvas();

        // Remove any redo steps
        if (this.historyStep < this.history.length - 1) {
            this.history.length = this.historyStep + 1;
        }

        this.history.push(dataCanvas.toDataURL());

        if (this.history.length > MAX_HISTORY) {
            this.history.shift();
        } else {
            this.historyStep++;
        }

        this.updateHistoryButtons();
    }

    undo() {
        if (this.historyStep > 0) {
            this.historyStep--;
            this.loadState(this.history[this.historyStep]);
        }
        this.updateHistoryButtons();
    }

    redo() {
        if (this.historyStep < this.history.length - 1) {
            this.historyStep++;
            this.loadState(this.history[this.historyStep]);
        }
        this.updateHistoryButtons();
    }

    loadState(dataUrl) {
        const restoreId = ++this._restoreSeq;

        const img = new Image();
        img.onload = () => {
            if (restoreId !== this._restoreSeq) return;

            this.canvasManager.loadImage(img);

            if (typeof this.onStateRestored === 'function') {
                this.onStateRestored();
            }
        };
        img.src = dataUrl;
    }

    updateHistoryButtons() {
        const undoBtn = document.getElementById('action-undo');
        const redoBtn = document.getElementById('action-redo');

        if (undoBtn) undoBtn.style.opacity = this.historyStep > 0 ? 1 : 0.3;
        if (redoBtn) redoBtn.style.opacity = this.historyStep < this.history.length - 1 ? 1 : 0.3;
    }

    canUndo() {
        return this.historyStep > 0;
    }

    canRedo() {
        return this.historyStep < this.history.length - 1;
    }
}
