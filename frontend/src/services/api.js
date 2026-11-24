
const API_ORIGIN = import.meta.env.VITE_API_ORIGIN?.trim() || '';

const allowedOrigin = new URL(
  API_ORIGIN || window.location.origin
);

if (!['http:', 'https:'].includes(allowedOrigin.protocol)) {
  throw new Error('Invalid API origin configuration.');
}

const GRAPHQL_ENDPOINT = `${allowedOrigin.origin}/graphql`;
const API_ENDPOINT = `${allowedOrigin.origin}/api`;

const STATIC_API_PATHS = new Set([
  '/auth/csrf',
  '/auth/me',
  '/auth/logout',
  '/auth/register',
  '/auth/login',
  '/history',
  '/history/searches',
  '/admin/users'
]);

let csrfToken = null;

export async function refreshCsrfToken() {
  const response = await fetch(`${API_ENDPOINT}/auth/csrf`, {
    credentials: 'include'
  });

  if (!response.ok) {
    throw new Error(
      `Could not initialize a secure session (${response.status}).`
    );
  }

  const data = await response.json();
  csrfToken = data.token;
}

export async function requestApi(path, options = {}) {
  const isUserPath =
    /^\/admin\/users\/[A-Za-z0-9_-]+$/.test(path);

  if (!STATIC_API_PATHS.has(path) && !isUserPath) {
    throw new Error('Unsupported API path.');
  }

  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});

  if (options.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    if (!csrfToken) {
      await refreshCsrfToken();
    }

    headers.set('X-XSRF-TOKEN', csrfToken);
  }

  const response = await fetch(`${API_ENDPOINT}${path}`, {
    ...options,
    method,
    headers,
    credentials: 'include'
  });

  const contentType = response.headers.get('content-type') || '';

  const data = contentType.includes('application/json')
    ? await response.json()
    : null;

  if (!response.ok) {
    const error = new Error(
      data?.detail ||
      data?.message ||
      `Request failed (${response.status}).`
    );

    error.status = response.status;
    throw error;
  }

  return data;
}

export const COMPARE_BASKET_QUERY = `
  query CompareBasket($items: [ItemInput!]!) {
    compareBasket(items: $items) {
      storeName
      totalRegularPrice
      totalClubPrice
      items {
        productName
        storePackageQuantity
        storePackageUnit
        calculatedRegularPrice
        calculatedClubPrice
      }
    }
  }
`;

export async function callGraphql(query, variables = {}) {
  try {
    if (!csrfToken) {
      await refreshCsrfToken();
    }

    const response = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken
      },
      body: JSON.stringify({ query, variables }),
      credentials: 'include'
    });

    const json = await response.json();

    if (!response.ok) {
      throw new Error(`GraphQL request failed (${response.status}).`);
    }

    if (json.errors) {
      throw new Error(json.errors[0].message);
    }

    return json.data;
  } catch (err) {
    console.error('GraphQL Error:', err);
    throw err;
  }
}
