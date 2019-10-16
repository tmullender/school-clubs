const { app, BrowserWindow, ipcMain } = require('electron');
const csv = require('csv-parser');
const log = require('electron-log');
const fs = require('fs');
const Store = require('electron-store');

const PREFERENCE_COUNT = 3;
const store = new Store();

String.prototype.capitalize = function() {
  return this.replace(/(?:^|\s)\S/g, function(a) { return a.toUpperCase(); });
};

function parseTime(timestamp) {
  let parts = timestamp.split(/[\/: ]/);
  let hours = "pm" == parts[6] ? +parts[3] + 12 : parts[3];
  return new Date(+parts[0], +parts[1]-1, +parts[2], hours, +parts[4], +parts[5]);
}

function parseClubs(row, clubs) {
  return Object.keys(row)
    .filter(key => !isNaN(key))
    .reduce((list, key, index) => { 
      let request = row[key];
      if (request.length == 0) {
        list.push(null);
      } else {
        let club = new Club(request, Math.ceil((1 + index)/PREFERENCE_COUNT));
        if (!clubs[club.key]) {
          clubs[club.key] = club;
        }
        list.push(club.key); 
      }
      return list; 
    }, []);
}

class Club {
  constructor(description, term) {
    let parts = description.split(/[()-]/)
    this.name = parts[0].trim().capitalize();
    this.staff = parts[2].trim();
    this.day = parts[1].trim();
    this.term = term;
    this.maximum = 30;
    this.key = this.name + this.day + this.term;
    this.description = this.name + ' (' + this.day + ')';
    this.allocated = [];
  }
}

class Pupil {
  constructor(row, clubs) {
    this.time = parseTime(row['Time']);
    this.timestamp = this.time.toLocaleString();
    this.name = row['Name'].trim();
    this.class = row['Class'];
    this.year = +this.class.charAt(1);
    this.index = this.time.getTime() - this.year * 1000*60*60*24*365; 
    this.count = +row['Count'];
    this.requests = parseClubs(row, clubs);
    this.allocated = [];
  }
}

function hasSpace(club) {
  return club.allocated.length < club.maximum;
}

function isFree(pupil, day, clubs) {
  return pupil.allocated.length < pupil.count && pupil.allocated.filter(clubKey => clubs[clubKey].day == day).length == 0;
}

function allocatePupil(pupil, clubs) {
  let success = pupil.requests.some(clubKey => {
    let club = clubs[clubKey];
    if (club && hasSpace(club) && isFree(pupil, club.day, clubs)){
      log.debug('Allocating ' + club.name + ' for ' + pupil.name);
      pupil.allocated.push(club.key);
      club.allocated.push(pupil.name);
      return true;
    }
    return false;
  });
  log.debug(pupil.name + ' allocated: ' + success);
}

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require('electron-squirrel-startup')) { // eslint-disable-line global-require
  app.quit();
}

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow;

const createWindow = () => {
  // Create the browser window.
  log.info('Creating window');
  mainWindow = new BrowserWindow({
    width: 800,
    height: 800,
    webPreferences: {
      preload: `${__dirname}/preload.js`
    }
  });

  // and load the index.html of the app.
  mainWindow.loadURL(`file://${__dirname}/index.html`);
  mainWindow.setMenuBarVisibility(false);

  // Emitted when the window is closed.
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
  log.info('Window created');
};

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow);

// Quit when all windows are closed.
app.on('window-all-closed', () => {
  // On OS X it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (mainWindow === null) {
    createWindow();
  }
});

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and import them here.
ipcMain.on('start-allocation', (event, input) => {
  log.info('Starting... ' + input);
  let pupils = {};
  let clubs = loadClubs();
  let classes = {};
  fs.createReadStream(input)
    .pipe(csv({ 
      headers: ['Time', 'Name', 'Class', '1', '2', '3', 'Count'],
      skipLines: 1
    }))
    .on('data', (row) => { 
      log.debug(row);
      let pupil = new Pupil(row, clubs);
      pupils[pupil.name] = pupil;
      if (!classes[pupil.class]) {
        classes[pupil.class] = []
      }
      classes[pupil.class].push(pupil.name);
    })
    .on('end', () => {
      log.debug('Sending allocation complete message');
      event.reply('read-complete', { pupils: pupils, clubs: clubs, classes: classes });
    });
});

ipcMain.on('allocate', (event, data) => {
  log.info('Allocating...');
  allocate(data.pupils, data.clubs);
  event.reply('allocation-complete', data);
});

function allocate(pupils, clubs) {
  let order = Object.values(pupils).sort((a, b) => { return a.index - b.index });
  for (let i = 0; i < PREFERENCE_COUNT; i++) {
    log.debug('Allocating iteration: ' + i);
    for (let pupil of order) {
      allocatePupil(pupil, clubs);
    }
    log.debug('Allocated iteration: ' + i);
  }
}

ipcMain.on('save-clubs', (event, data) => {
  saveClubs(data);
})

function loadClubs() {
  return store.get('clubs', {});
}

function saveClubs(clubs) {
  store.set('clubs', clubs);
  log.info(store.path);
}